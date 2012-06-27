package aurelienribon.libgdx.polygoneditor;

import aurelienribon.libgdx.ImageModel;
import aurelienribon.libgdx.ImageModel.Shape;
import aurelienribon.libgdx.polygoneditor.input.PanZoomInputProcessor;
import aurelienribon.libgdx.polygoneditor.input.ShapeEditInputProcessor;
import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenManager;
import aurelienribon.utils.gdx.Label;
import aurelienribon.utils.gdx.Label.Anchor;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Canvas extends ApplicationAdapter {
	private final TweenManager tweenManager = new TweenManager();
	private CanvasDrawer drawer;
	private SpriteBatch batch;
	private BitmapFont font;
	private Sprite infoLabel;
	private Texture bgTex;
	private Sprite sprite;

	private final List<Label> labels = new ArrayList<Label>();
	private Label lblModeCreation;
	private Label lblModeEdition;
	private Label lblClearVertices;
	private Label lblInsertVertices;
	private Label lblRemoveVertices;

	public OrthographicCamera camera;
	public ImageModel selectedModel;
	public final List<Vector2> selectedPoints = new ArrayList<Vector2>();
	public Vector2 nearestPoint, nextPoint;
	public Vector2 mouseSelectionP1, mouseSelectionP2;
	public boolean drawTriangles = true, drawBoundingBox = true;
	public float spriteOpacity = 0.5f;

	public static enum Mode {CREATION, EDITION}
	public Mode mode = null;

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	@Override
	public void create() {
		Assets.loadAll();

		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();

		camera = new OrthographicCamera(w, h);
		batch = new SpriteBatch();
		font = new BitmapFont();
		drawer = new CanvasDrawer(camera);

		infoLabel = new Sprite(Assets.getWhiteTex());
		infoLabel.setPosition(0, 0);
		infoLabel.setSize(110, 55);
		infoLabel.setColor(new Color(0x2A/255f, 0x3B/255f, 0x56/255f, 180/255f));

		bgTex = Assets.getTransparentLightTex();
		bgTex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

		int lblH = 25;
		Color lblC = new Color(0x2A/255f, 0x6B/255f, 0x56/255f, 180/255f);
		lblModeCreation = new Label(10+lblH, 80, lblH, "Creation", font, lblC, Anchor.TOP_LEFT);
		lblModeEdition = new Label(10+lblH*2, 80, lblH, "Edition", font, lblC, Anchor.TOP_LEFT);
		lblClearVertices = new Label(10+lblH*1, 120, lblH, "Clear all points", font, lblC, Anchor.TOP_RIGHT);
		lblRemoveVertices = new Label(15+lblH*2, 120, lblH, "Remove points", font, lblC, Anchor.TOP_RIGHT);
		lblInsertVertices = new Label(20+lblH*3, 120, lblH, "Insert points", font, lblC, Anchor.TOP_RIGHT);
		labels.addAll(Arrays.asList(lblModeCreation, lblModeEdition, lblClearVertices, lblInsertVertices, lblRemoveVertices));

		Label.TouchCallback modeLblCallback = new Label.TouchCallback() {
			@Override public void touchDown(Label source) {
				nextMode();
				lblModeCreation.tiltOff();
				lblModeEdition.tiltOff();
			}
			@Override public void touchEnter(Label source) {
				lblModeCreation.tiltOn();
				lblModeEdition.tiltOn();
			}
			@Override public void touchExit(Label source) {
				lblModeCreation.tiltOff();
				lblModeEdition.tiltOff();
			}
		};

		lblModeCreation.setCallback(modeLblCallback);
		lblModeEdition.setCallback(modeLblCallback);

		lblClearVertices.setCallback(new Label.TouchCallback() {
			@Override public void touchDown(Label source) {clearPoints();}
		});

		lblInsertVertices.setCallback(new Label.TouchCallback() {
			@Override public void touchDown(Label source) {insertPointsBetweenSelected();}
		});

		lblRemoveVertices.setCallback(new Label.TouchCallback() {
			@Override public void touchDown(Label source) {removeSelectedPoints();}
		});

		InputMultiplexer im = new InputMultiplexer();
		im.addProcessor(modeInputProcessor);
		im.addProcessor(buttonsInputProcessor);
		im.addProcessor(new PanZoomInputProcessor(this));
		im.addProcessor(new ShapeEditInputProcessor(this));
		Gdx.input.setInputProcessor(im);

		Tween.call(new TweenCallback() {
			@Override public void onEvent(int type, BaseTween<?> source) {updateButtons();}
		}).repeat(-1, 0.3f).start(tweenManager);
	}

	private final InputProcessor modeInputProcessor = new InputAdapter() {
		@Override
		public boolean keyDown(int keycode) {
			if (selectedModel != null) {
				if (keycode == Input.Keys.TAB) nextMode();
			}

			return false;
		}
	};

	private final InputProcessor buttonsInputProcessor = new InputAdapter() {
		@Override
		public boolean touchDown(int x, int y, int pointer, int button) {
			if (button == Input.Buttons.LEFT) for (Label label : labels) if (label.touchDown(x, y)) return true;
			return false;
		}

		@Override
		public boolean touchMoved(int x, int y) {
			for (Label label : labels) label.touchMoved(x, y);
			return false;
		}
	};

	@Override
	public void render() {
		tweenManager.update(Gdx.graphics.getDeltaTime());

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		float tw = bgTex.getWidth();
		float th = bgTex.getHeight();

		batch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
		batch.begin();
		batch.disableBlending();
		batch.draw(bgTex, 0f, 0f, w, h, 0f, 0f, w/tw, h/th);
		batch.enableBlending();
		batch.end();

		if (selectedModel != null) {
			batch.setProjectionMatrix(camera.combined);
			batch.begin();
			sprite.setColor(1, 1, 1, spriteOpacity);
			sprite.draw(batch);
			batch.end();

			if (drawBoundingBox) drawer.drawBoundingBox(sprite);
			drawer.drawModel(selectedModel, selectedPoints, nextPoint, nearestPoint, drawTriangles, mode == Mode.CREATION);
			drawer.drawMouseSelection(mouseSelectionP1, mouseSelectionP2);
		}

		batch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
		batch.begin();
		font.setColor(Color.WHITE);
		infoLabel.draw(batch);
		font.draw(batch, String.format(Locale.US, "Zoom: %.0f %%", 100f / camera.zoom), 10, 45);
		font.draw(batch, "Fps: " + Gdx.graphics.getFramesPerSecond(), 10, 25);
		for (Label lbl : labels) lbl.draw(batch);
		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		Gdx.gl.glViewport(0, 0, width, height);
		camera.viewportWidth = width;
		camera.viewportHeight = height;
		camera.update();
	}

	public Vector2 screenToWorld(int x, int y) {
		Vector3 v3 = new Vector3(x, y, 0);
		camera.unproject(v3);
		return new Vector2(v3.x, v3.y);
	}

	public void setImage(ImageModel img) {
		selectedModel = img;

		if (img == null) {sprite = null; return;}
		if (sprite != null) sprite.getTexture().dispose();

		Texture tex = new Texture(Gdx.files.absolute(img.file.getPath()));
		tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		sprite = new Sprite(tex);

		camera.position.set(sprite.getWidth()/2, sprite.getHeight()/2, 0);
		camera.zoom = 1;
		camera.update();
	}

	// -------------------------------------------------------------------------
	// Internals
	// -------------------------------------------------------------------------

	private void updateButtons() {
		if (selectedModel != null && mode == null) setMode(Mode.CREATION);
		if (selectedModel == null && mode != null) setMode(null);
		if (isClearEnabled()) lblClearVertices.show(); else lblClearVertices.hide();
		if (isRemoveEnabled()) lblRemoveVertices.show(); else lblRemoveVertices.hide();
		if (isInsertEnabled()) lblInsertVertices.show(); else lblInsertVertices.hide();
	}

	private void clearPoints() {
		if (selectedModel == null) return;
		selectedPoints.clear();
		selectedModel.clear();
	}

	private void insertPointsBetweenSelected() {
		if (!isInsertEnabled()) return;
		List<Vector2> toAdd = new ArrayList<Vector2>();

		for (Shape shape : selectedModel.shapes) {
			List<Vector2> vs = shape.vertices;

			for (int i=0; i<vs.size(); i++) {
				Vector2 p1 = vs.get(i);
				Vector2 p2 = i != vs.size()-1 ? vs.get(i+1) : vs.get(0);

				if (selectedPoints.contains(p1) && selectedPoints.contains(p2)) {
					Vector2 p = new Vector2((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
					vs.add(i+1, p);
					toAdd.add(p);
				}
			}
		}

		selectedPoints.addAll(toAdd);
		selectedModel.triangulate();
	}

	private void removeSelectedPoints() {
		if (!isRemoveEnabled()) return;

		for (int i=selectedModel.shapes.size()-1; i>=0; i--) {
			List<Vector2> vs = selectedModel.shapes.get(i).vertices;
			for (Vector2 p : selectedPoints) if (vs.contains(p)) vs.remove(p);
			if (vs.size() < 3) selectedModel.shapes.remove(i);
		}

		selectedPoints.clear();
		selectedModel.triangulate();
	}

	private boolean isClearEnabled() {
		if (selectedModel == null) return false;
		return !selectedModel.shapes.isEmpty();
	}

	private boolean isInsertEnabled() {
		if (selectedModel == null) return false;
		if (selectedPoints.size() <= 1) return false;

		for (Shape shape : selectedModel.shapes) {
			Vector2 v1 = null;
			for (Vector2 v2 : shape.vertices) {
				if (v1 != null && selectedPoints.contains(v2)) return true;
				v1 = selectedPoints.contains(v2) ? v2 : null;
			}
			if (v1 != null && selectedPoints.contains(shape.vertices.get(0))) return true;
		}

		return false;
	}

	private boolean isRemoveEnabled() {
		if (selectedModel == null) return false;
		return !selectedPoints.isEmpty();
	}

	private void nextMode() {
		Mode m = mode == Mode.CREATION ? Mode.EDITION : Mode.CREATION;
		setMode(m);
	}

	private void setMode(Mode mode) {
		this.mode = mode;

		selectedPoints.clear();
		nextPoint = null;
		nearestPoint = null;

		if (mode == null) {
			lblModeCreation.hide();
			lblModeEdition.hide();
		} else {
			lblModeCreation.hideSemi();
			lblModeEdition.hideSemi();
			switch (mode) {
				case CREATION: lblModeCreation.show(); break;
				case EDITION: lblModeEdition.show(); break;
			}
		}
	}
}
