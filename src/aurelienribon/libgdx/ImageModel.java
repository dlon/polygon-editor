package aurelienribon.libgdx;

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
	public final List<Vector2> vertices = new ArrayList<Vector2>();
	public final List<Vector2> trianglesVertices = new ArrayList<Vector2>();
	public boolean closed = false;
	private final float w, h;

	public ImageModel(File file) throws IOException {
		this.file = file.getCanonicalFile();

		BufferedImage img = ImageIO.read(file);
		w = img.getWidth();
		h = img.getHeight();
	}

	public void triangulate() {
		clearTriangles();
		if (vertices.size() < 3 || !closed) return;

		EarClippingTriangulator ect = new EarClippingTriangulator();
		List<Vector2> triangles = ect.computeTriangles(vertices);
		trianglesVertices.addAll(triangles);
	}

	public void clear() {
		vertices.clear();
		trianglesVertices.clear();
		closed = false;
	}

	public void clearTriangles() {
		trianglesVertices.clear();
	}

	public List<Vector2> getUVs() {
		if (w < 0.1f || h < 0.1f) return new ArrayList<Vector2>();
		List<Vector2> uvs = new ArrayList<Vector2>();
		for (Vector2 v : trianglesVertices) {
			uvs.add(new Vector2(v.x/w, 1-v.y/h));
		}
		return uvs;
	}
}
