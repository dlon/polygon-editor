package aurelienribon.libgdx.polygoneditor;

import aurelienribon.utils.notifications.ChangeableObject;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Vector2;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public class ImageModel extends ChangeableObject {
	public final File file;
	public final List<ImageModel.Shape> shapes = new ArrayList<ImageModel.Shape>();
	public final List<Vector2> trianglesVertices = new ArrayList<Vector2>();
	public final List<Vector2> trianglesUVs = new ArrayList<Vector2>();
	private final float w, h;

	public ImageModel(File file) throws IOException {
		this.file = file.getCanonicalFile();

		BufferedImage img = ImageIO.read(file);
		w = img.getWidth();
		h = img.getHeight();
	}

	public static class Shape {
		public final List<Vector2> vertices = new ArrayList<Vector2>();
		public boolean closed;
	}

	public void triangulate() {
		clearTriangles();
		EarClippingTriangulator ect = new EarClippingTriangulator();

		for (ImageModel.Shape shape : shapes) {
			if (shape.vertices.size() < 3 || !shape.closed) continue;
			trianglesVertices.addAll(ect.computeTriangles(shape.vertices));
		}

		if (w > 0.1f && h > 0.1f) {
			for (Vector2 v : trianglesVertices) trianglesUVs.add(new Vector2(v.x/w, 1-v.y/h));
		}
	}

	public void clear() {
		shapes.clear();
		clearTriangles();
	}

	public void clearTriangles() {
		trianglesVertices.clear();
		trianglesUVs.clear();
	}
}
