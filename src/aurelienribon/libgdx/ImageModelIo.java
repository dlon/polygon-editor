package aurelienribon.libgdx;

import aurelienribon.utils.io.FilenameHelper;
import com.badlogic.gdx.math.Vector2;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public class ImageModelIo {
	public static void save(File file, List<ImageModel> models) throws IOException {
		String output = "";

		for (int i=0; i<models.size(); i++) {
			if (i > 0) output += "\n\n---\n\n";
			models.get(i).triangulate();

			output += "i " + FilenameHelper.relativize(models.get(i).file.getPath(), file.getParent());

			output += "\ns ";
			List<Vector2> vs = models.get(i).vertices;
			for (Vector2 v : vs) output += (v == vs.get(0) ? "" : ",") + v.x + "," + v.y;

			output += "\nv ";
			List<Vector2> tvs = models.get(i).trianglesVertices;
			for (Vector2 v : tvs) output += (v == tvs.get(0) ? "" : ",") + v.x + "," + v.y;

			output += "\nu ";
			List<Vector2> uvs = models.get(i).getUVs();
			for (Vector2 v : uvs) output += (v == uvs.get(0) ? "" : ",") + v.x + "," + v.y;
		}

		FileUtils.writeStringToFile(file, output);
	}

	public static List<ImageModel> load(File file) throws IOException {
		String input = FileUtils.readFileToString(file);
		List<ImageModel> models = new ArrayList<ImageModel>();

		String[] descs = input.split("---");

		for (String desc : descs) {
			String[] lines = desc.trim().split("\n");
			String path = parseBlock(lines, "i");
			String vs = parseBlock(lines, "s");

			ImageModel model = new ImageModel(new File(file.getParent(), path));
			model.vertices.addAll(parseVertices(vs));
			model.closed = model.vertices.size() >= 3;
			model.triangulate();
			models.add(model);
		}

		return models;
	}

	private static String parseBlock(String[] lines, String start) {
		for (String line : lines) if (line.startsWith(start)) return line.substring(start.length()).trim();
		return "";
	}

	private static List<Vector2> parseVertices(String input) {
		List<Vector2> vs = new ArrayList<Vector2>();
		String[] words = input.split(",");
		for (int i=1; i<words.length; i+=2) {
			float x = Float.parseFloat(words[i-1]);
			float y = Float.parseFloat(words[i]);
			vs.add(new Vector2(x, y));
		}
		return vs;
	}
}
