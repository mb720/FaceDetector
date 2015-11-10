package eu.matthiasbraun;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_INTER_LINEAR;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvEqualizeHist;
import static org.bytedeco.javacpp.opencv_imgproc.cvResize;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static org.bytedeco.javacpp.opencv_objdetect.cvLoadHaarClassifierCascade;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaceDetector {

	private static final String FACE_CASCADE_FILE_PATH = "/haarcascade_frontalface_default.xml";
	private static final Logger LOG = LoggerFactory.getLogger(FaceDetector.class);

	private static Optional<CvHaarClassifierCascade> cascadeMaybe = loadFaceCascade();

	public static List<FaceDetectionResult> detectFaces(List<BufferedImage> images) {

		List<FaceDetectionResult> results = new ArrayList<>();
		images.forEach(image -> {
			final Optional<FaceDetectionResult> resultMaybe = detect(image);
			resultMaybe.ifPresent(results::add);
		});
		return results;
	}

	private static Optional<FaceDetectionResult> detect(BufferedImage bufImage) {

		FaceDetectionResult result = null;
		if (cascadeMaybe.isPresent()) {
			CvHaarClassifierCascade cascade = cascadeMaybe.get();

			try (IplImage image = toSmallerGrayScale(toIplImage(bufImage))) {

				CvMemStorage storage = CvMemStorage.create();
				final double scaleFactor = 1.5;
				final int minNeighbors = 3;
				CvSeq detectedFaces = cvHaarDetectObjects(image, cascade, storage, scaleFactor, minNeighbors, CV_HAAR_DO_CANNY_PRUNING);
				cvClearMemStorage(storage);

				List<Rectangle> rectangles = getFaceRectangles(detectedFaces);
				int nrOfFacesInImage = detectedFaces.total();
				result = new FaceDetectionResult(bufImage, nrOfFacesInImage, rectangles);
			}
			catch (Exception e) {
				LOG.warn("Exception while detecting images: {}", e);
			}
		}
		return Optional.ofNullable(result);
	}

	private static List<Rectangle> getFaceRectangles(CvSeq detectedFacesInImage) {
		List<Rectangle> rectangles = new ArrayList<>();
		int nrOfFaces = detectedFacesInImage.total();
		for (int i = 0; i < nrOfFaces; i++) {
			try (CvRect cvRect = new CvRect(cvGetSeqElem(detectedFacesInImage, i))) {
				Rectangle rect = new Rectangle(cvRect.x(), cvRect.y(), cvRect.width(), cvRect.height());
				rectangles.add(rect);
			}
			catch (Exception e) {
				LOG.info("Could not get rectangle around face from image", e);
			}
		}
		return rectangles;
	}

	private static Optional<CvHaarClassifierCascade> loadFaceCascade() {
		File file = LittleIOUtil.getResource(FACE_CASCADE_FILE_PATH, FaceDetector.class);
		CvHaarClassifierCascade faceCascade = null;
		if (file.exists()) {
			faceCascade = cvLoadHaarClassifierCascade(file.getAbsolutePath(), cvSize(0, 0));
		}
		else {
			LOG.warn("Can't load face cascade file since it doesn't exist: {}", file);
		}
		return Optional.ofNullable(faceCascade);
	}

	private static IplImage toIplImage(BufferedImage bufImage) {

		ToIplImage iplConverter = new OpenCVFrameConverter.ToIplImage();
		Java2DFrameConverter java2dConverter = new Java2DFrameConverter();
		IplImage iplImage = iplConverter.convert(java2dConverter.convert(bufImage));
		return iplImage;
	}

	private static IplImage toSmallerGrayScale(IplImage origImg) {
		// This results in more faces being found than with the unaltered image
		IplImage grayImg = IplImage.create(origImg.width(), origImg.height(), IPL_DEPTH_8U, 1);
		cvCvtColor(origImg, grayImg, CV_BGR2GRAY);

		// Additionally scaling down the image speeds up detection
		int scaleFactor = 2;
		IplImage smallImg = IplImage.create(grayImg.width() / scaleFactor, grayImg.height() / scaleFactor, IPL_DEPTH_8U, 1);
		cvResize(grayImg, smallImg, CV_INTER_LINEAR);

		IplImage equalizedImg = IplImage.create(smallImg.width(), smallImg.height(), IPL_DEPTH_8U, 1);
		cvEqualizeHist(smallImg, equalizedImg);
		return equalizedImg;
	}
}
