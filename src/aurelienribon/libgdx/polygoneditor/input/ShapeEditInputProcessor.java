package aurelienribon.libgdx.polygoneditor.input;

import aurelienribon.libgdx.ImageModel;
import aurelienribon.libgdx.ImageModel.Shape;
import aurelienribon.libgdx.polygoneditor.Canvas;
import aurelienribon.libgdx.polygoneditor.InputHelper;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public class ShapeEditInputProcessor extends InputAdapter {
	private final Canvas canvas;
	private final Shape emptyShape = new Shape() {{closed = true;}};
	private boolean touchDown = false;

	public ShapeEditInputProcessor(Canvas canvas) {
		this.canvas = canvas;
	}

	@Override
	public boolean touchDown(int x, int y, int pointer, int button) {
		touchDown = button == Input.Buttons.LEFT;
		if (!touchDown || canvas.selectedModel == null) return false;

		ImageModel model = canvas.selectedModel;
		Shape lastShape = model.shapes.isEmpty() ? emptyShape : model.shapes.get(model.shapes.size()-1);
		List<Vector2> vs = lastShape.vertices;
		List<Vector2> ss = canvas.selectedPoints;
		Vector2 p = canvas.screenToWorld(x, y);

		canvas.mouseSelectionP1 = null;

		switch (canvas.mode) {
			case CREATION:
				if (lastShape.closed) {
					Shape shape = new Shape();
					shape.vertices.add(p);
					model.shapes.add(shape);
				} else {
					if (vs.size() > 2 && canvas.nearestPoint == vs.get(0)) {
						lastShape.closed = true;
						model.triangulate();
					} else {
						lastShape.vertices.add(p);
					}
				}
				break;

			case EDITION:
				if (canvas.nearestPoint != null) {
					if (InputHelper.isCtrlDown()) {
						if (ss.contains(canvas.nearestPoint)) ss.remove(canvas.nearestPoint);
						else ss.add(canvas.nearestPoint);
					} else if (!ss.contains(canvas.nearestPoint)) {
						ss.clear();
						ss.add(canvas.nearestPoint);
					}
				} else {
					if (!InputHelper.isCtrlDown()) ss.clear();
					canvas.mouseSelectionP1 = p;
					canvas.mouseSelectionP2 = p;
				}
				break;
		}

		return false;
	}

	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		if (!touchDown || canvas.selectedModel == null) return false;
		touchDown = false;

		ImageModel model = canvas.selectedModel;
		List<Vector2> ss = canvas.selectedPoints;

		switch (canvas.mode) {
			case EDITION:
				if (canvas.mouseSelectionP1 != null && InputHelper.isCtrlDown()) {
					for (Vector2 p : getPointsInSelection()) {
						if (ss.contains(p)) ss.remove(p);
						else ss.add(p);
					}
				} else if (canvas.mouseSelectionP1 != null) {
					ss.clear();
					ss.addAll(getPointsInSelection());
				}

				canvas.mouseSelectionP1 = null;
				model.triangulate();
				break;
		}

		return false;
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		if (!touchDown || canvas.selectedModel == null) return false;

		ImageModel model = canvas.selectedModel;
		List<Vector2> ss = canvas.selectedPoints;
		Vector2 p = canvas.screenToWorld(x, y);

		switch (canvas.mode) {
			case CREATION:
				break;

			case EDITION:
				if (canvas.nearestPoint != null && !InputHelper.isCtrlDown()) {
					float dx = p.x - canvas.nearestPoint.x;
					float dy = p.y - canvas.nearestPoint.y;
					for (Vector2 pp : ss) pp.add(dx, dy);
				} else {
					canvas.mouseSelectionP2 = p;
				}
				model.trianglesVertices.clear();
				break;
		}

		return false;
	}

	@Override
	public boolean touchMoved(int x, int y) {
		if (canvas.selectedModel == null) return false;

		// Nearest point computation

		Vector2 p = canvas.screenToWorld(x, y);
		canvas.nextPoint = p;
		canvas.nearestPoint = null;
		float dist = 10 * canvas.camera.zoom;

		for (Vector2 v : getAllPoints()) {
			if (v.dst(p) < dist) canvas.nearestPoint = v;
		}

		return false;
	}

	// -------------------------------------------------------------------------

	private List<Vector2> getPointsInSelection() {
		List<Vector2> points = new ArrayList<Vector2>();
		Vector2 p1 = canvas.mouseSelectionP1;
		Vector2 p2 = canvas.mouseSelectionP2;

		if (p1 != null && p2 != null) {
			Rectangle rect = new Rectangle(
				Math.min(p1.x, p2.x),
				Math.min(p1.y, p2.y),
				Math.abs(p2.x - p1.x),
				Math.abs(p2.y - p1.y)
			);

			for (Vector2 p : getAllPoints()) {
				if (rect.contains(p.x, p.y)) points.add(p);
			}
		}

		return Collections.unmodifiableList(points);
	}

	private List<Vector2> getAllPoints() {
		ImageModel model = canvas.selectedModel;
		List<Vector2> points = new ArrayList<Vector2>();
		for (Shape shape : model.shapes) points.addAll(shape.vertices);
		return Collections.unmodifiableList(points);
	}
}
