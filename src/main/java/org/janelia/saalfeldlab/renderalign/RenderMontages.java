/**
 *
 */
package org.janelia.saalfeldlab.renderalign;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.janelia.alignment.Render;
import org.janelia.alignment.RenderParameters;
import org.janelia.alignment.Utils;
import org.janelia.alignment.json.JsonUtils;
import org.janelia.alignment.util.ImageProcessorCache;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ZProjector;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import mpicbg.util.Timer;
import scala.Tuple2;

/**
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 *
 */
public class RenderMontages {

	public static class Options implements Serializable {

		@Option(name = "-S", aliases = {"--server"}, required = true, usage = "Server base URL.")
		private String server = "http://tem-services.int.janelia.org:8080/render-ws/v1";

		@Option(name = "-u", aliases = {"--owner", "--user"}, required = true, usage = "Owner.")
		private String owner = "flyTEM";

		@Option(name = "-p", aliases = {"--project"}, required = true, usage = "Project ID.")
		private String projectId = "FAFB00";

		@Option(name = "-s", aliases = {"--stack"}, required = true, usage = "Stack ID.")
		private String stackId = "v5_align_tps";

		@Option(name = "-x", aliases = {"--x"}, required = false, usage = "Left most pixel coordinate in world coordinates.  Default is bounds.minX of the stack.")
		private Double x = null;

		@Option(name = "-y", aliases = {"--y"}, required = false, usage = "Top most pixel coordinate in world coordinates.  Default is bounds.minY of the stack.")
		private Double y = null;

		@Option(name = "-w", aliases = {"--width"}, required = false, usage = "Width in world coordinates.  Default is bounds.maxX - x of the stack.")
		private Double w = null;

		@Option(name = "-h", aliases = {"--height"}, required = false, usage = "Height in world coordinates.  Default is bounds.maxY - y of the stack.")
		private Double h = null;

		@Option(name = "-t", aliases = {"--scale"}, required = true, usage = "Scale.")
		private Double scale = 1.0 / 8.0;

		@Option(name = "-o", aliases = {"--outputpath"}, required = true, usage = "Output path.")
		private String outputPath = "/tmp/";

		@Option(name = "-z", aliases = {"--zscale"}, required = false, usage = "Z-scale.")
		private Double zScale = 1.0;

	    private boolean parsedSuccessfully = false;

		public Options(final String[] args) {
			final CmdLineParser parser = new CmdLineParser(this);
			try {
				parser.parseArgument(args);
				parsedSuccessfully = true;
			} catch (final CmdLineException e) {
				System.err.println(e.getMessage());
				parser.printUsage(System.err);
			}

			if (!outputPath.endsWith("/"))
				outputPath += "/";
		}

		public String getServer() {
			return server;
		}

		public String getOwner() {
			return owner;
		}

		public String getProjectId() {
			return projectId;
		}

		public String getStackId() {
			return stackId;
		}

		public Double getX() {
			return x;
		}

		public Double getY() {
			return y;
		}

		public Double getW() {
			return w;
		}

		public Double getH() {
			return h;
		}

		public Double getScale() {
			return scale;
		}

		public boolean isParsedSuccessfully() {
			return parsedSuccessfully;
		}
	}

	public static void main(final String... args) throws IllegalArgumentException, IOException, InterruptedException, ExecutionException {

		System.out.println("*************** Job started! ***************");

		final Options options = new Options(args);

		final String baseUrlString = options.getServer() +
                "/owner/" + options.getOwner() +
                "/project/" + options.getProjectId() +
                "/stack/" + options.getStackId();

		System.out.println("Fetching z-indices from render database...");
		System.out.println(baseUrlString);

		final URL zValuesUrl = new URL(baseUrlString + "/zValues");
		final JsonUtils.Helper<Double> jsonHelper = new JsonUtils.Helper<>(Double.class);
		List<Double> zs = jsonHelper.fromJsonArray(new InputStreamReader(zValuesUrl.openStream()));

		/* recover after breaking jobs */
		/* exclude successfully rendered images */
		final File outputDir = new File(options.outputPath);
		final List<String> files = Arrays.asList(
				outputDir.list(
					new FilenameFilter() {

						@Override
						public boolean accept(final File dir, final String name) {
							return name.endsWith(".png");
						}
					}));

		final ArrayList<Double> zsTBD = new ArrayList<Double>();
		for (final Double zValue : zs ) {
			if (!files.contains(zValue.toString() + ".png"))
				zsTBD.add(zValue);
		}

		Collections.sort(zs);
		zs = zsTBD;

		final double zScale = options.zScale / options.scale;
		final ArrayList<ArrayList<Double>> zTuples = new ArrayList<>();
		ArrayList<Double> currentZTuple = new ArrayList<>();
		zTuples.add(currentZTuple);
		double currentZ = zs.get(0).doubleValue();
		for (final Double z : zs) {
			if (z - zScale >= currentZ) {
				currentZ = z;
				currentZTuple = new ArrayList<>();
				zTuples.add(currentZTuple);
			}
			currentZTuple.add(z);
		}

		System.out.println("Stack z-indices TBD (" + zs.size() + "):");

		final String urlString =
				baseUrlString +
				"/z/%s/render-parameters";

		final SparkConf conf      = new SparkConf().setAppName( "RenderMontages" );
        final JavaSparkContext sc = new JavaSparkContext(conf);

		final JavaRDD<ArrayList<Double>> rddZ = sc.parallelize(zTuples);

		rddZ.cache();
		System.out.println("rddZ count = " + rddZ.count());

		final JavaPairRDD<Double, ArrayList<String>> urls = rddZ.mapToPair(
				new PairFunction<ArrayList<Double>, Double, ArrayList<String>>() {

					@Override
					public Tuple2<Double, ArrayList<String>> call(final ArrayList<Double> zs) throws Exception {
						final ArrayList<String> urls = new ArrayList<>();
						for (final Double z : zs) {
							if (
									options.getX() == null ||
									options.getY() == null ||
									options.getW() == null ||
									options.getH() == null)
								urls.add(String.format(String.format(urlString, "%f"), z));
							else
								urls.add(
										String.format(
											String.format(urlString, "%f/box/%f,%f,%d,%d,%f"),
											z,
											options.getX(),
											options.getY(),
											options.getW().intValue(),
											options.getH().intValue(),
											options.getScale()));
						}
						return new Tuple2<Double, ArrayList<String> >(zs.get(0), urls);
					}
				});

		urls.cache();
		System.out.println("urls count = " + urls.count());

		final JavaPairRDD<Double, ArrayList<RenderParameters>> parameters = urls.mapToPair(
				new PairFunction<Tuple2<Double, ArrayList<String>>, Double, ArrayList<RenderParameters>>() {

					@Override
					public Tuple2<Double, ArrayList<RenderParameters>> call(final Tuple2<Double, ArrayList<String>> pair) throws Exception {
						final ArrayList<RenderParameters> parametersList = new ArrayList<>();
						for (final String url : pair._2()) {
							final RenderParameters parameters = RenderParameters.parseJson(new InputStreamReader(new URL(url).openStream()));

							parameters.setScale(options.scale);
							parameters.initializeDerivedValues();
							parametersList.add(parameters);
						}

						return new Tuple2<Double, ArrayList<RenderParameters>>(
								pair._1(),
								parametersList);
					}
				});

		parameters.cache();

		final JavaRDD<Double> images = parameters.map(
				new Function<Tuple2<Double, ArrayList<RenderParameters>>, Double>() {

					@Override
					public Double call(final Tuple2<Double, ArrayList<RenderParameters>> pair) throws Exception {

						final ArrayList<RenderParameters> parametersList = pair._2();
						final RenderParameters firstParameters = parametersList.get(0);
						final BufferedImage box = firstParameters.openTargetImage();
						final ImageStack stack = new ImageStack(box.getWidth(), box.getHeight());
						for (final RenderParameters param : parametersList) {

							final BufferedImage targetImage = param.openTargetImage();
							final ByteProcessor ip = new ByteProcessor(targetImage.getWidth(), targetImage.getHeight());

							targetImage.getGraphics().drawImage(ip.createImage(), 0, 0, null);

							final Timer timer = new Timer();
							timer.start();
							try {
	        					Render.render(
	        							param.getTileSpecs(),
	        							targetImage,
	        							options.x,
	        							options.y,
	        							param.getRes(param.getScale()),
	        							param.getScale(),
	        							false,
	        							1,
	        							false,
	        							false,
	        							ImageProcessorCache.DISABLED_CACHE,
	        							0);
							} catch (final IndexOutOfBoundsException e) {
							    System.out.println("Failed rendering layer " + pair._1() + " because");
							    e.printStackTrace(System.out);
							}
							timer.stop();

							stack.addSlice(new ColorProcessor(targetImage).convertToByteProcessor());
						}

						final ZProjector projector = new ZProjector(new ImagePlus("", stack));
						projector.setMethod( ZProjector.AVG_METHOD );
						projector.doProjection();

						final ImageProcessor ip = projector.getProjection().getProcessor();

						final BufferedImage targetImage = ip.getBufferedImage();
						final String fileName = String.format("%s%08.1f%s", options.outputPath, pair._1(), ".png");
						System.out.println(fileName);
						Utils.saveImage(
								targetImage,
								fileName,
								"png",
								true,
								9);

						return pair._1();
					}
				});

		System.out.println("images count = " + images.count());

		System.out.println("Done.");

        sc.close();

	}
}
