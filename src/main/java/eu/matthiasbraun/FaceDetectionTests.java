package eu.matthiasbraun;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

public class FaceDetectionTests {
	private static final Logger LOG = LoggerFactory.getLogger(FaceDetectionTests.class);

	public static void main(String[] args) {
		String testImageDir = "/testImages";
		List<BufferedImage> images = loadImagesFromResources(testImageDir);

		try {
			Stopwatch watch = Stopwatch.createStarted();
			List<FaceDetectionResult> results = FaceDetector.detectFaces(images);

			Integer totalNrOfFaces = results.stream().mapToInt(FaceDetectionResult::getNrOfFaces).sum();
			LOG.info("Detecting {} faces in {} images took {}", totalNrOfFaces, images.size(), watch);
		}
		// JavaCV throws an UnsatisfiedLinkError, which is not an Exception but a Throwable, if the platform specific library is not on the PATH
		catch (Throwable e) {
			LOG.warn("Something went wrong while detecting faces: {}", e.getMessage());
		}
	}

	private static List<BufferedImage> loadImagesFromDrive(Path imageDir) {
		List<BufferedImage> images = new ArrayList<>();
		boolean recursive = false;
		List<Path> paths = LittleIOUtil.listFiles(imageDir, recursive, LittleIOUtil.MATCH_ALL);
		paths.forEach(path -> {
			try {
				BufferedImage image = ImageIO.read(path.toFile());
				images.add(image);
			}
			catch (Exception e) {
				LOG.warn("Can't load image {}", path, e);
			}
		});
		return images;
	}

	private static List<BufferedImage> loadImagesFromResources(String imageDir) {
		List<BufferedImage> images = new ArrayList<>();
		List<String> paths = LittleIOUtil.flatten(imageDir);
		paths.forEach(path -> {
			try {
				BufferedImage image = ImageIO.read(new File(path));
				images.add(image);
			}
			catch (Exception e) {
				LOG.warn("Can't load image {}", path, e);
			}
		});
		return images;
	}
}
