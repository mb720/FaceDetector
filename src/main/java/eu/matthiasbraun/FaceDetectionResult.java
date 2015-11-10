package eu.matthiasbraun;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

public class FaceDetectionResult {

	private int nrOfFacesInImage;
	private BufferedImage image;
	private List<Rectangle> rectanglesAroundFaces;

	public FaceDetectionResult(BufferedImage image, int nrOfFacesInImage, List<Rectangle> rectsAroundFaces) {
		this.image = image;
		this.nrOfFacesInImage = nrOfFacesInImage;
		this.rectanglesAroundFaces = rectsAroundFaces;
	}

	public int getNrOfFaces() {
		return nrOfFacesInImage;
	}

	@Override
	public String toString() {
		return "FaceDetectionResult [nrOfFacesInImage=" + nrOfFacesInImage + ", image=" + image + ", rectanglesAroundFaces=" + rectanglesAroundFaces + "]";
	}

}
