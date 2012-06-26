package aurelienribon.libgdx.polygoneditor.input;

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
	private boolean touchDown = false;

	public ShapeEditInputProcessor(Canvas canvas) {
		this.canvas = canvas;
	}

	@Override
	public boolean touchDown(int x, int y, int pointer, int button) {
		touchDown = button == Input.Buttons.LEFT;
		if (!touchDown || canvas.selectedModel == null) return false;

		List<Vector2> vs = canvas.selectedModel.vertices;
		List<Vector2> ps = canvas.selectedPoints;
		Vector2 p = canvas.screenToWorld(x, y);

		canvas.mouseSelectionP1 = null;

		if (!canvas.selectedModel.closed) {
			if (vs.size() > 2 && canvas.nearestPoint == vs.get(0)) {
				canvas.selectedModel.closed = true;
			} else {
				canvas.selectedModel.vertices.add(p);
			}
		} else {
			if (canvas.nearestPoint != null) {
				if (InputHelper.isCtrlDown()) {
					if (ps.contains(canvas.nearestPoint)) ps.remove(canvas.nearestPoint);
					else ps.add(canvas.nearestPoint);
				} else if (!ps.contains(canvas.nearestPoint)) {
					ps.clear();
					ps.add(canvas.nearestPoint);
				}
			} else {
				if (!InputHelper.isCtrlDown()) ps.clear();
				canvas.mouseSelectionP1 = p;
				canvas.mouseSelectionP2 = p;
			}
		}

		return false;
	}

	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		if (!touchDown || canvas.selectedModel == null) return false;
		touchDown = false;

		if (canvas.mouseSelectionP1 != null && InputHelper.isCtrlDown()) {
			for (Vector2 p : getPointsInSelection()) {
				if (canvas.selectedPoints.contains(p)) canvas.selectedPoints.remove(p);
				else canvas.selectedPoints.add(p);
			}
		} else if (canvas.mouseSelectionP1 != null) {
			canvas.selectedPoints.clear();
			canvas.selectedPoints.addAll(getPointsInSelection());
		}

		canvas.mouseSelectionP1 = null;
		canvas.selectedModel.triangulate();
		return false;
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		if (!touchDown || canvas.selectedModel == null) return false;

		Vector2 p = canvas.screenToWorld(x, y);

		if (canvas.nearestPoint != null && !InputHelper.isCtrlDown()) {
			float dx = p.x - canvas.nearestPoint.x;
			float dy = p.y - canvas.nearestPoint.y;
			for (Vector2 pp : canvas.selectedPoints) pp.add(dx, dy);
		} else {
			canvas.mouseSelectionP2 = p;
		}

		canvas.selectedModel.trianglesVertices.clear();
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

		for (Vector2 v : canvas.selectedModel.vertices) {
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

			for (Vector2 p : canvas.selectedModel.vertices) {
				if (rect.contains(p.x, p.y)) points.add(p);
			}
		}

		return Collections.unmodifiableList(points);
	}
}
