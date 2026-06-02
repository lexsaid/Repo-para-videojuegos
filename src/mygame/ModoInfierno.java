package mygame;
 
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.scene.shape.Quad;
 
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
 
/**
 * =========================================================
 *  MODO INFIERNO — Cooperativo local 2 jugadores
 * =========================================================
 *  Jugador 1 (Personaje_1) — mecánica Abanico (3 balas)
 *    Movimiento : Flechas  ↑ ↓ ← →
 *    Disparar   : Numpad + (KEY_ADD)
 *    Recargar   : - (KEY_MINUS)      ← P1 recarga con -
 *
 *  Jugador 2 (Personaje_2) — mecánica Zombies (1 bala con dispersión)
 *    Movimiento : WASD
 *    Disparar   : ESPACIO (mantenido)
 *    Recargar   : R                  ← P2 recarga con R
 *
 *  · Cada jugador: 100 balas, 3 vidas
 *  · Game Over global cuando los DOS están muertos
 *  · Solo Enemigo_2 (tipo 1) dispara
 *  · Spawn: 1.5 s  |  Máximo: 50
 * =========================================================
 */
public class ModoInfierno implements GameMode {
 
    // ── Constantes ──────────────────────────────────────────
    // Equivalentes a las de ModoBase pero declaradas localmente porque
    // ModoInfierno no hereda de ModoBase.
    // SHOOT_CD_P1/P2: cooldown de disparo de cada jugador (P2 es más rápido).
    // ENEMY_SHOOT_TIME: segundos entre disparos de los enemigos tipo 1.
    // MAX_ENEMIGOS y ENEMY_SPAWN_TIME son más agresivos que en los otros modos.
    private static final float ARENA_SIZE         = 20f;
    private static final float JUGADOR_MARGEN     = 0.35f;
    private static final float BULLET_SPEED       = 14f;
    private static final float BULLET_LIFETIME    = 3f;
    private static final float ENEMY_BULLET_SPEED = 7f;
    private static final float INVENCIBILITY_TIME = 2f;
    private static final float PARPADEO_INTERVAL  = 0.15f;
    private static final float VEL_ANIM           = 0.15f;
    private static final float VEL_ANIM_ENEMY     = 0.12f;
    private static final int   PUNTOS_POR_ENEMIGO = 100;
    private static final int   MAX_ENEMIGOS       = 100;  // más enemigos en pantalla
    private static final float ENEMY_SPAWN_TIME   = 0.2f; // spawn más frecuente
    private static final float ENEMY_SPEED        = 1.4f;
    private static final float SHOOT_CD_P1        = 0.25f;
    private static final float SHOOT_CD_P2        = 0.12f;
    private static final float ENEMY_SHOOT_TIME   = 2.5f; // cadencia disparo Enemigo_2
 
    // ── Recursos jME ────────────────────────────────────────
    // Nodos separados para las balas de cada jugador (bulletsNode1 y bulletsNode2)
    // para facilitar la detección de colisiones: bala de P1 da puntos a P1, etc.
    private Node         rootNode, guiNode, modoRoot, modoGui;
    private Node         enemiesNode, bulletsNode1, bulletsNode2, enemyBulletsNode;
    private AssetManager assetManager;
    private InputManager inputManager;
    private Camera       cam;
    private BitmapFont   guiFont;
    private AudioNode    musicaFondo;
 
    // ── Estado global ────────────────────────────────────────
    // score: total combinado de P1 + P2.
    // scoreP1 / scoreP2: puntos individuales de cada jugador.
    // spawnTimer: acumula tpf hasta ENEMY_SPAWN_TIME para generar el siguiente enemigo.
    private boolean gameOver   = false;
    private int     score      = 0;
    private int     scoreP1    = 0;
    private int     scoreP2    = 0;
    private float   spawnTimer = 0f;
 
    private final List<BulletData> enemyBullets = new ArrayList<>();
    
    private Material materialBalaJugador;
    private Material materialBalaEnemigo;
 
    // ══════════════════════════════════════════════════════
    //  JUGADOR 1 — Personaje_1, flechas + NUM+, -
    // ══════════════════════════════════════════════════════
    private Geometry  p1Geo;
    private Material  p1Mat;
    private Texture[] p1AnimD = new Texture[5];
    private Texture[] p1AnimI = new Texture[5];
    private Vector3f  p1Dir   = new Vector3f(1, 0, 0);
    private String    p1UltDir = "DERECHA";
    private float     p1AnimTimer = 0f;
    private int       p1AnimFrame = 0;
 
    private int     p1Vidas         = 3;
    private boolean p1Muerto        = false;
    private boolean p1Invencible    = false;
    private float   p1InvTimer      = 0f;
    private float   p1ParpadeoTimer = 0f;
    private boolean p1Visible       = true;
    private int     p1Balas         = 100;
    private float   p1ShootTimer    = SHOOT_CD_P1;
 
    private boolean p1Left, p1Right, p1Up, p1Down, p1Disparando;
    private final List<BulletData> p1Bullets = new ArrayList<>();
 
    // ══════════════════════════════════════════════════════
    //  JUGADOR 2 — Personaje_2, WASD + ESPACIO, R
    // ══════════════════════════════════════════════════════
    private Geometry  p2Geo;
    private Material  p2Mat;
    private Texture[] p2AnimD = new Texture[4];
    private Texture[] p2AnimI = new Texture[4];
    private Vector3f  p2Dir   = new Vector3f(-1, 0, 0);
    private String    p2UltDir = "IZQUIERDA";
    private float     p2AnimTimer = 0f;
    private int       p2AnimFrame = 0;
 
    private int     p2Vidas         = 3;
    private boolean p2Muerto        = false;
    private boolean p2Invencible    = false;
    private float   p2InvTimer      = 0f;
    private float   p2ParpadeoTimer = 0f;
    private boolean p2Visible       = true;
    private int     p2Balas         = 100;
    private float   p2ShootTimer    = SHOOT_CD_P2;
 
    private boolean p2Left, p2Right, p2Up, p2Down, p2Disparando;
    private final List<BulletData> p2Bullets = new ArrayList<>();
 
    // ══════════════════════════════════════════════════════
    //  ENEMIGOS
    // ══════════════════════════════════════════════════════
    private final List<EnemyData> enemies = new ArrayList<>();
    private Material[][] matEnemigoD = new Material[4][];
    private Material[][] matEnemigoI = new Material[4][];
 
    // ── HUD ─────────────────────────────────────────────────
    private BitmapText hudP1Vidas, hudP1Balas, hudP1Estado, hudP1Score;
    private BitmapText hudP2Vidas, hudP2Balas, hudP2Estado, hudP2Score;
    private BitmapText hudScore;
    private BitmapText hudGameOver, hudReinicio;
 
    // ══════════════════════════════════════════════════════
    //  CICLO DE VIDA
    // ══════════════════════════════════════════════════════
 
    @Override
    public void iniciar(Node rootNode, Node guiNode,
                        AssetManager assetManager,
                        InputManager inputManager,
                        Camera cam, BitmapFont guiFont) {
 
        this.rootNode     = rootNode;
        this.guiNode      = guiNode;
        this.assetManager = assetManager;
        this.inputManager = inputManager;
        this.cam          = cam;
        this.guiFont      = guiFont;
 
        modoRoot = new Node("infiernoRoot");
        modoGui  = new Node("infiernoGui");
        rootNode.attachChild(modoRoot);
        guiNode.attachChild(modoGui);
 
        cam.setLocation(new Vector3f(0, 20, 0));
        cam.lookAt(Vector3f.ZERO, new Vector3f(0, 0, -1));
 
        enemiesNode      = new Node("enemies");
        bulletsNode1     = new Node("bullets1");
        bulletsNode2     = new Node("bullets2");
        enemyBulletsNode = new Node("enemyBullets");
        modoRoot.attachChild(enemiesNode);
        modoRoot.attachChild(bulletsNode1);
        modoRoot.attachChild(bulletsNode2);
        modoRoot.attachChild(enemyBulletsNode);
 
        cargarTexturas();
        
        // Cargar texturas de balas
        Texture texBalaJugador = assetManager.loadTexture("Textures/Balas/Bala_1.png");
        materialBalaJugador = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        materialBalaJugador.setTexture("ColorMap", texBalaJugador);
        materialBalaJugador.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        Texture texBalaEnemigo = assetManager.loadTexture("Textures/Balas/Bala_0.png");
        materialBalaEnemigo = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        materialBalaEnemigo.setTexture("ColorMap", texBalaEnemigo);
        materialBalaEnemigo.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        
        setupArena();
        setupJugadores();
        setupHUD();
        registrarInput();
 
        for (int i = 0; i < 6; i++) spawnEnemy();
 
        musicaFondo = new AudioNode(assetManager, "Sounds/musica6.wav", DataType.Buffer);
        musicaFondo.setLooping(true);
        musicaFondo.setVolume(0.5f);
        musicaFondo.setPositional(false);
        rootNode.attachChild(musicaFondo);
        musicaFondo.play();
    }
 
    @Override
    public void update(float tpf) {
        if (gameOver) return;
 
        if (!p1Muerto) {
            moverJugador1(tpf);
            if (p1Disparando) intentarDisparar1();
            if (p1ShootTimer < SHOOT_CD_P1) p1ShootTimer += tpf;
        }
        if (!p2Muerto) {
            moverJugador2(tpf);
            if (p2Disparando) intentarDisparar2();
            if (p2ShootTimer < SHOOT_CD_P2) p2ShootTimer += tpf;
        }
 
        moverEnemigos(tpf);
        actualizarDisparoEnemigos(tpf);   // ← solo Enemigo_2 dispara
        moverListaBalas(tpf, p1Bullets, bulletsNode1);
        moverListaBalas(tpf, p2Bullets, bulletsNode2);
        moverBalasEnemigas(tpf);
        detectarColisiones();
        detectarColisionBalasEnemigas();  // ← balas enemigas vs jugadores
 
        spawnTimer += tpf;
        if (spawnTimer >= ENEMY_SPAWN_TIME && enemies.size() < MAX_ENEMIGOS) {
            spawnEnemy();
            spawnTimer = 0f;
        }
 
        actualizarAnimP1(tpf);
        actualizarAnimP2(tpf);
        actualizarCamara();
        actualizarHUD();
 
        if (p1Muerto && p2Muerto && !gameOver) activarGameOver();
    }
 
    /**
     * Cámara dinámica para 2 jugadores:
     *  - Si ambos viven: se centra entre los dos y hace zoom-out dinámico
     *    según la distancia que los separa (altura entre 20 y 40 unidades).
     *  - Si solo uno vive: sigue a ese jugador con zoom fijo.
     *  - Si ambos muertos: la cámara queda estática donde estaba.
     */
    private void actualizarCamara() {
        Vector3f centro;
        if (!p1Muerto && !p2Muerto) {
            centro = p1Geo.getLocalTranslation().add(p2Geo.getLocalTranslation()).mult(0.5f);
        } else if (!p1Muerto) {
            centro = p1Geo.getLocalTranslation().clone();
        } else if (!p2Muerto) {
            centro = p2Geo.getLocalTranslation().clone();
        } else {
            return;
        }
        float distancia = (!p1Muerto && !p2Muerto)
                ? p1Geo.getLocalTranslation().distance(p2Geo.getLocalTranslation()) : 0f;
        float alturaDin = FastMath.clamp(20f + distancia * 0.8f, 20f, 40f);
        float lim = ARENA_SIZE - 7f;
        centro.x = FastMath.clamp(centro.x, -lim, lim);
        centro.z = FastMath.clamp(centro.z, -lim, lim);
        cam.setLocation(new Vector3f(centro.x, alturaDin, centro.z));
        cam.lookAt(new Vector3f(centro.x, 0, centro.z), new Vector3f(0, 0, -1));
    }
 
    @Override
    public void onReiniciar() {
        limpiarEntidades();
 
        p1Vidas = 3; p1Muerto = false; p1Invencible = false;
        p1InvTimer = 0f; p1Balas = 100; p1ShootTimer = SHOOT_CD_P1;
        p1Dir = new Vector3f(1, 0, 0); p1UltDir = "DERECHA";
        p1Geo.setLocalTranslation(-3f, 0.3f, 0f);
        p1Mat.setColor("Color", ColorRGBA.White);
        p1Geo.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
 
        p2Vidas = 3; p2Muerto = false; p2Invencible = false;
        p2InvTimer = 0f; p2Balas = 100; p2ShootTimer = SHOOT_CD_P2;
        p2Dir = new Vector3f(-1, 0, 0); p2UltDir = "IZQUIERDA";
        p2Geo.setLocalTranslation(3f, 0.3f, 0f);
        p2Mat.setColor("Color", ColorRGBA.White);
        p2Geo.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
 
        gameOver = false; spawnTimer = 0f;
        scoreP1 = 0; scoreP2 = 0; score = 0;
 
        hudGameOver.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        hudReinicio.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
 
        for (int i = 0; i < 6; i++) spawnEnemy();
    }
 
    @Override
    public void destruir() {
        if (musicaFondo != null) {
            musicaFondo.stop();
            rootNode.detachChild(musicaFondo);
            musicaFondo = null;
        }
        limpiarEntidades();
        modoRoot.removeFromParent();
        modoGui.removeFromParent();
        try { inputManager.removeListener(inputListener); } catch (Exception ignored) {}
    }
 
    @Override public int     getScore()          { return score; }
    @Override public boolean isGameOver()        { return gameOver; }
    @Override public String  getNombreModo()     { return "ModoInfierno"; }
    @Override public int     getBalasRestantes() { return -1; }
 
    // ══════════════════════════════════════════════════════
    //  CARGA DE TEXTURAS
    // ══════════════════════════════════════════════════════
 
    private void cargarTexturas() {
        for (int i = 0; i < 5; i++) {
            p1AnimD[i] = assetManager.loadTexture("Textures/Personaje_1/PjRight_" + i + ".png");
            p1AnimI[i] = assetManager.loadTexture("Textures/Personaje_1/PjLeft_"  + i + ".png");
        }
        for (int i = 0; i < 4; i++) {
            p2AnimD[i] = assetManager.loadTexture("Textures/Personaje_2/Pj2Right_" + (i + 1) + ".png");
            p2AnimI[i] = assetManager.loadTexture("Textures/Personaje_2/Pj2Left_"  + (i + 1) + ".png");
        }
        // Tipo 0: Enemigo_1 (5 frames)
        matEnemigoD[0] = new Material[5]; matEnemigoI[0] = new Material[5];
        for (int i = 0; i < 5; i++) {
            matEnemigoD[0][i] = matTex("Textures/Enemigo_1/enemy_"     + i + ".png");
            matEnemigoI[0][i] = matTex("Textures/Enemigo_1/EnemyLeft_" + i + ".png");
        }
        // Tipo 1: Enemigo_2 (4 frames) — ES EL ÚNICO QUE DISPARA
        matEnemigoD[1] = new Material[4]; matEnemigoI[1] = new Material[4];
        for (int i = 0; i < 4; i++) {
            matEnemigoD[1][i] = matTex("Textures/Enemigo_2/Enemy2Right_" + i + ".png");
            matEnemigoI[1][i] = matTex("Textures/Enemigo_2/Enemy2Left_"  + i + ".png");
        }
        // Tipo 2: Enemigo_3 (4 frames)
        matEnemigoD[2] = new Material[4]; matEnemigoI[2] = new Material[4];
        for (int i = 0; i < 4; i++) {
            matEnemigoD[2][i] = matTex("Textures/Enemigo_3/PjRight_" + i + ".png");
            matEnemigoI[2][i] = matTex("Textures/Enemigo_3/PjLeft_"  + i + ".png");
        }
        // Tipo 3: variante de Enemigo_2 (mismas texturas, velocidad distinta)
        matEnemigoD[3] = matEnemigoD[1];
        matEnemigoI[3] = matEnemigoI[1];
    }
 
    // ══════════════════════════════════════════════════════
    //  ESCENA
    // ══════════════════════════════════════════════════════
 
    private void setupArena() {
        Box shape = new Box(ARENA_SIZE, 0.1f, ARENA_SIZE);
        Geometry arena = new Geometry("arena", shape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = assetManager.loadTexture("Textures/Fondo/Escenario4.png");
        tex.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("ColorMap", tex);
        arena.setMaterial(mat);
        arena.setLocalTranslation(0, -0.1f, 0);
        modoRoot.attachChild(arena);
    }
 
    private void setupJugadores() {
        Box shape1 = new Box(0.8f, 0.01f, 1f);
        p1Geo = new Geometry("p1", shape1);
        p1Mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        p1Mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        p1Mat.setTexture("ColorMap", p1AnimD[0]);
        p1Geo.setMaterial(p1Mat);
        p1Geo.setQueueBucket(RenderQueue.Bucket.Transparent);
        p1Geo.rotate(0, 4.7f, 0);
        p1Geo.setLocalScale(1f, 1f, 1f);
        p1Geo.setLocalTranslation(-3f, 0.3f, 0f);
        modoRoot.attachChild(p1Geo);
 
        p2Mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        p2Mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        p2Mat.setTexture("ColorMap", p2AnimI[0]);
        p2Mat.setColor("Color", ColorRGBA.White);
        p2Geo = new Geometry("p2", new Box(0.8f, 0.01f, 1f));
        p2Geo.setMaterial(p2Mat);
        p2Geo.setQueueBucket(RenderQueue.Bucket.Transparent);
        p2Geo.rotate(0, 4.7f, 0);
        p2Geo.setLocalScale(1f);
        p2Geo.setLocalTranslation(3f, 0.3f, 0f);
        modoRoot.attachChild(p2Geo);
    }
 
    // ══════════════════════════════════════════════════════
    //  HUD
    // ══════════════════════════════════════════════════════
 
    private void setupHUD() {
        float W  = cam.getWidth();
        float H  = cam.getHeight();
        float fs = guiFont.getCharSet().getRenderedSize();
 
        // ── Jugador 1 (izquierda) ──
        hudP1Vidas = htext("", fs * 1.3f, ColorRGBA.Red);
        hudP1Vidas.setLocalTranslation(10, H - 10, 0);
        modoGui.attachChild(hudP1Vidas);
 
        hudP1Balas = htext("", fs * 1.3f, ColorRGBA.Cyan);
        hudP1Balas.setLocalTranslation(10, H - 32, 0);
        modoGui.attachChild(hudP1Balas);
 
        // Controles P1 corregidos: flechas + NUM+ dispara + MENOS recarga
        hudP1Estado = htext("P1: Flechas=mover  NUM[SHIFT_DERECHO]=disparar  P=recargar", fs * 0.85f,
                new ColorRGBA(0.8f, 0.8f, 0.8f, 1f));
        hudP1Estado.setLocalTranslation(10, H - 52, 0);
        modoGui.attachChild(hudP1Estado);
 
        hudP1Score = htext("P1 Pts: 0", fs * 1.3f, ColorRGBA.Yellow);
        hudP1Score.setLocalTranslation(10, H - 74, 0);
        modoGui.attachChild(hudP1Score);
 
        // ── Jugador 2 (derecha) ──
        hudP2Vidas = htext("", fs * 1.3f, ColorRGBA.Orange);
        modoGui.attachChild(hudP2Vidas);
 
        hudP2Balas = htext("", fs * 1.3f, ColorRGBA.Green);
        modoGui.attachChild(hudP2Balas);
 
        // Controles P2 corregidos: WASD mueve + ESPACIO dispara + R recarga
        hudP2Estado = htext("P2: WASD=mover  ESPACIO=disparar  R=recargar", fs * 0.85f,
                new ColorRGBA(0.8f, 0.8f, 0.8f, 1f));
        modoGui.attachChild(hudP2Estado);
 
        hudP2Score = htext("P2 Pts: 0", fs * 1.3f, ColorRGBA.Yellow);
        modoGui.attachChild(hudP2Score);
 
        hudScore = htext("Puntos: 0", fs * 1.4f, ColorRGBA.Yellow);
        hudScore.setLocalTranslation((W - 120) / 2f, H - 10, 0);
        modoGui.attachChild(hudScore);
 
        hudGameOver = htext("GAME OVER", fs * 3f, ColorRGBA.Red);
        hudGameOver.setLocalTranslation((W - hudGameOver.getLineWidth()) / 2f, H / 2f + 60, 0);
        hudGameOver.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        modoGui.attachChild(hudGameOver);
 
        hudReinicio = htext("R → Reiniciar   |   ESC → Volver al menú", fs * 1.1f, ColorRGBA.White);
        hudReinicio.setLocalTranslation((W - hudReinicio.getLineWidth()) / 2f, H / 2f + 15, 0);
        hudReinicio.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        modoGui.attachChild(hudReinicio);
 
        actualizarHUD();
    }
 
    private void actualizarHUD() {
        float W  = cam.getWidth();
        float H  = cam.getHeight();
 
        hudP1Vidas.setText("P1 Vidas: " + p1Vidas + (p1Muerto ? " [MUERTO]" : ""));
        hudP1Balas.setText("P1 Balas: " + p1Balas);
        hudP1Balas.setColor(p1Balas > 20 ? ColorRGBA.Cyan : ColorRGBA.Red);
        hudP1Score.setText("P1 Pts: " + scoreP1);
 
        hudP2Vidas.setText("P2 Vidas: " + p2Vidas + (p2Muerto ? " [MUERTO]" : ""));
        hudP2Vidas.setLocalTranslation(W - hudP2Vidas.getLineWidth() - 10, H - 10, 0);
        hudP2Balas.setText("P2 Balas: " + p2Balas);
        hudP2Balas.setColor(p2Balas > 20 ? ColorRGBA.Green : ColorRGBA.Red);
        hudP2Balas.setLocalTranslation(W - hudP2Balas.getLineWidth() - 10, H - 32, 0);
        hudP2Estado.setLocalTranslation(W - hudP2Estado.getLineWidth() - 10, H - 52, 0);
        hudP2Score.setText("P2 Pts: " + scoreP2);
        hudP2Score.setLocalTranslation(W - hudP2Score.getLineWidth() - 10, H - 74, 0);
 
        hudScore.setText("Total: " + score);
    }
 
    // ══════════════════════════════════════════════════════
    //  MOVIMIENTO JUGADORES
    // ══════════════════════════════════════════════════════
 
    private void moverJugador1(float tpf) {
        Vector3f pos = p1Geo.getLocalTranslation().clone();
        Vector3f dir = new Vector3f();
        if (p1Left)  dir.x -= 1;
        if (p1Right) dir.x += 1;
        if (p1Up)    dir.z -= 1;
        if (p1Down)  dir.z += 1;
        if (dir.lengthSquared() > 0) {
            dir.normalizeLocal();
            p1Dir = dir.clone();
            pos.addLocal(dir.mult(5.5f * tpf));
        }
        float lim = ARENA_SIZE - JUGADOR_MARGEN;
        pos.x = FastMath.clamp(pos.x, -lim, lim);
        pos.z = FastMath.clamp(pos.z, -lim, lim);
        pos.y = 0.3f;
        p1Geo.setLocalTranslation(pos);
        p1Geo.setLocalScale(1f);
 
        if (p1Invencible) {
            p1InvTimer -= tpf; p1ParpadeoTimer -= tpf;
            if (p1ParpadeoTimer <= 0) {
                p1Visible = !p1Visible;
                p1ParpadeoTimer = PARPADEO_INTERVAL;
                p1Mat.setColor("Color", p1Visible ? ColorRGBA.White : ColorRGBA.Blue);
            }
            if (p1InvTimer <= 0) { p1Invencible = false; p1Mat.setColor("Color", ColorRGBA.White); }
        }
    }
 
    private void moverJugador2(float tpf) {
        Vector3f pos = p2Geo.getLocalTranslation().clone();
        Vector3f dir = new Vector3f();
        if (p2Left)  dir.x -= 1;
        if (p2Right) dir.x += 1;
        if (p2Up)    dir.z -= 1;
        if (p2Down)  dir.z += 1;
        if (dir.lengthSquared() > 0) {
            dir.normalizeLocal();
            p2Dir = dir.clone();
            pos.addLocal(dir.mult(6.5f * tpf));
        }
        float lim = ARENA_SIZE - JUGADOR_MARGEN;
        pos.x = FastMath.clamp(pos.x, -lim, lim);
        pos.z = FastMath.clamp(pos.z, -lim, lim);
        pos.y = 0.3f;
        p2Geo.setLocalTranslation(pos);
        p2Geo.setLocalScale(1f);
 
        if (p2Invencible) {
            p2InvTimer -= tpf; p2ParpadeoTimer -= tpf;
            if (p2ParpadeoTimer <= 0) {
                p2Visible = !p2Visible;
                p2ParpadeoTimer = PARPADEO_INTERVAL;
                p2Mat.setColor("Color", p2Visible ? ColorRGBA.White : ColorRGBA.Orange);
            }
            if (p2InvTimer <= 0) { p2Invencible = false; p2Mat.setColor("Color", ColorRGBA.White); }
        }
    }
 
    // ══════════════════════════════════════════════════════
    //  ANIMACIÓN JUGADORES
    // ══════════════════════════════════════════════════════
 
    private void actualizarAnimP1(float tpf) {
        boolean mov = p1Left || p1Right || p1Up || p1Down;
        if (!mov) {
            p1Mat.setTexture("ColorMap", p1UltDir.equals("IZQUIERDA") ? p1AnimI[0] : p1AnimD[0]);
            p1Mat.setColor("Color", ColorRGBA.White);
            p1AnimFrame = 0; p1AnimTimer = 0f; return;
        }
        if      (p1Right) p1UltDir = "DERECHA";
        else if (p1Left)  p1UltDir = "IZQUIERDA";
        else if (p1Up)    p1UltDir = "DERECHA";
        else              p1UltDir = "IZQUIERDA";
        p1AnimTimer += tpf;
        if (p1AnimTimer >= VEL_ANIM) {
            p1AnimTimer = 0f;
            p1AnimFrame = (p1AnimFrame + 1 >= p1AnimD.length) ? 1 : p1AnimFrame + 1;
            p1Mat.setTexture("ColorMap", p1UltDir.equals("DERECHA") ? p1AnimD[p1AnimFrame] : p1AnimI[p1AnimFrame]);
            p1Mat.setColor("Color", ColorRGBA.White);
        }
    }
 
    private void actualizarAnimP2(float tpf) {
        boolean mov = p2Left || p2Right || p2Up || p2Down;
        if (!mov) {
            p2Mat.setTexture("ColorMap", p2UltDir.equals("IZQUIERDA") ? p2AnimI[0] : p2AnimD[0]);
            p2Mat.setColor("Color", ColorRGBA.White);
            p2AnimFrame = 0; p2AnimTimer = 0f; return;
        }
        if      (p2Right) p2UltDir = "DERECHA";
        else if (p2Left)  p2UltDir = "IZQUIERDA";
        else if (p2Up)    p2UltDir = "DERECHA";
        else              p2UltDir = "IZQUIERDA";
        p2AnimTimer += tpf;
        if (p2AnimTimer >= VEL_ANIM) {
            p2AnimTimer = 0f;
            p2AnimFrame = (p2AnimFrame + 1 >= p2AnimD.length) ? 1 : p2AnimFrame + 1;
            p2Mat.setTexture("ColorMap", p2UltDir.equals("DERECHA") ? p2AnimD[p2AnimFrame] : p2AnimI[p2AnimFrame]);
            p2Mat.setColor("Color", ColorRGBA.White);
        }
    }
 
    // ══════════════════════════════════════════════════════
    //  DISPARO JUGADORES
    // ══════════════════════════════════════════════════════
 
    private void intentarDisparar1() {
        if (p1ShootTimer < SHOOT_CD_P1 || p1Balas <= 0) return;
        p1ShootTimer = 0f;
        for (float angulo : new float[]{-25f, 0f, 25f}) {
            Quaternion rot = new Quaternion();
            rot.fromAngleAxis(FastMath.DEG_TO_RAD * angulo, Vector3f.UNIT_Y);
            crearBala(p1Geo.getLocalTranslation().clone(), rot.mult(p1Dir).normalizeLocal(),
                    bulletsNode1, p1Bullets, ColorRGBA.Yellow);
        }
        p1Balas--;
    }
 
    private void intentarDisparar2() {
        if (p2ShootTimer < SHOOT_CD_P2 || p2Balas <= 0) return;
        p2ShootTimer = 0f;
        float dispersion = (float)(Math.random() * 30f) - 15f;
        Quaternion rot = new Quaternion();
        rot.fromAngleAxis(FastMath.DEG_TO_RAD * dispersion, Vector3f.UNIT_Y);
        crearBala(p2Geo.getLocalTranslation().clone(), rot.mult(p2Dir).normalizeLocal(),
                bulletsNode2, p2Bullets, ColorRGBA.Cyan);
        p2Balas--;
    }
 
    private void crearBala(Vector3f origen, Vector3f dir,
                        Node nodo, List<BulletData> lista, ColorRGBA color) {
        Quad shape = new Quad(0.7f, 0.35f);
        Geometry g = new Geometry("bala", shape);
        g.center();
        orientarBalaInfierno(g, dir.normalize());

        Material mat = materialBalaJugador.clone();
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        g.setMaterial(mat);
        g.setQueueBucket(RenderQueue.Bucket.Transparent);

        Vector3f pos = origen.clone();
        pos.y = 0.25f;
        g.setLocalTranslation(pos);
        nodo.attachChild(g);
        lista.add(new BulletData(g, dir, BULLET_LIFETIME));
    }

    /** Orienta un Quad horizontal apuntando en la dirección de vuelo (vista top-down). */
    private void orientarBalaInfierno(Geometry g, Vector3f dir) {
        float angulo = FastMath.atan2(dir.z, dir.x);
        Quaternion tumbado = new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
        Quaternion giro    = new Quaternion().fromAngleAxis(-angulo, Vector3f.UNIT_Y);
        g.setLocalRotation(giro.mult(tumbado));
    }
 
    // ══════════════════════════════════════════════════════
    //  DISPARO ENEMIGOS — solo Enemigo_2 (tipo 1 y tipo 3)
    // ══════════════════════════════════════════════════════
 
    private void actualizarDisparoEnemigos(float tpf) {
        // Determinar objetivo más cercano vivo
        for (EnemyData e : enemies) {
            // Solo disparan los de tipo 1 (Enemigo_2)
            if (e.tipo != 1) continue;
 
            e.shootTimer += tpf;
            if (e.shootTimer < ENEMY_SHOOT_TIME) continue;
            e.shootTimer = 0f;
 
            // Persigue al jugador vivo más cercano
            Vector3f objetivo = null;
            float distMin = Float.MAX_VALUE;
            if (!p1Muerto) {
                float d = e.geo.getLocalTranslation().distance(p1Geo.getLocalTranslation());
                if (d < distMin) { distMin = d; objetivo = p1Geo.getLocalTranslation(); }
            }
            if (!p2Muerto) {
                float d = e.geo.getLocalTranslation().distance(p2Geo.getLocalTranslation());
                if (d < distMin) { objetivo = p2Geo.getLocalTranslation(); }
            }
            if (objetivo == null) continue;
 
            Vector3f dir = objetivo.subtract(e.geo.getLocalTranslation());
            if (dir.lengthSquared() <= 0.01f) continue;
            dir.normalizeLocal();
 
            Quad eShape = new Quad(0.7f, 0.35f);
            Geometry g = new Geometry("eBullet", eShape);
            g.center();
            orientarBalaInfierno(g, dir);

            Material matE = materialBalaEnemigo.clone();
            matE.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            g.setMaterial(matE);
            g.setQueueBucket(RenderQueue.Bucket.Transparent);

            Vector3f eOrigen = e.geo.getLocalTranslation().clone();
            eOrigen.y = 0.25f;
            g.setLocalTranslation(eOrigen);
            enemyBulletsNode.attachChild(g);
            enemyBullets.add(new BulletData(g, dir, BULLET_LIFETIME));
        }
    }
 
    private void moverListaBalas(float tpf, List<BulletData> lista, Node nodo) {
        Iterator<BulletData> it = lista.iterator();
        while (it.hasNext()) {
            BulletData b = it.next();
            b.lifetime -= tpf;
            if (b.lifetime <= 0) { nodo.detachChild(b.geo); it.remove(); continue; }
            Vector3f p = b.geo.getLocalTranslation().add(b.dir.mult(BULLET_SPEED * tpf));
            p.y = 0.25f;
            b.geo.setLocalTranslation(p);
            if (Math.abs(p.x) > ARENA_SIZE + 1 || Math.abs(p.z) > ARENA_SIZE + 1) {
                nodo.detachChild(b.geo); it.remove();
            }
        }
    }
 
    private void moverBalasEnemigas(float tpf) {
        Iterator<BulletData> it = enemyBullets.iterator();
        while (it.hasNext()) {
            BulletData b = it.next();
            b.lifetime -= tpf;
            if (b.lifetime <= 0) { enemyBulletsNode.detachChild(b.geo); it.remove(); continue; }
            Vector3f p = b.geo.getLocalTranslation().add(b.dir.mult(ENEMY_BULLET_SPEED * tpf));
            p.y = 0.25f;
            b.geo.setLocalTranslation(p);
            if (Math.abs(p.x) > ARENA_SIZE + 1 || Math.abs(p.z) > ARENA_SIZE + 1) {
                enemyBulletsNode.detachChild(b.geo); it.remove();
            }
        }
    }
 
    private void detectarColisionBalasEnemigas() {
        if (gameOver) return;
        List<BulletData> rb = new ArrayList<>();
        for (BulletData b : enemyBullets) {
            Vector3f bp = b.geo.getLocalTranslation();
            if (!p1Muerto && !p1Invencible && bp.distance(p1Geo.getLocalTranslation()) < 0.45f) {
                rb.add(b); recibirDanioP1();
            } else if (!p2Muerto && !p2Invencible && bp.distance(p2Geo.getLocalTranslation()) < 0.45f) {
                rb.add(b); recibirDanioP2();
            }
        }
        for (BulletData b : rb) { enemyBulletsNode.detachChild(b.geo); enemyBullets.remove(b); }
    }
 
    // ══════════════════════════════════════════════════════
    //  ENEMIGOS
    // ══════════════════════════════════════════════════════
 
    private void spawnEnemy() {
        int tipo = FastMath.rand.nextInt(4);
        Geometry g = new Geometry("enemy", new Box(0.8f, 0f, 1f));
        g.setMaterial(matEnemigoD[tipo][0]);
        g.setQueueBucket(RenderQueue.Bucket.Transparent);
        g.rotate(0, 4.7f, 0);
        float edge = ARENA_SIZE - 0.55f;
        float x, z;
        switch (FastMath.rand.nextInt(4)) {
            case 0  -> { x = rnd(edge); z = -edge; }
            case 1  -> { x = rnd(edge); z =  edge; }
            case 2  -> { x = -edge;     z = rnd(edge); }
            default -> { x =  edge;     z = rnd(edge); }
        }
        g.setLocalTranslation(x, 0.3f, z);
        enemiesNode.attachChild(g);
        float vel = ENEMY_SPEED + tipo * 0.15f;
        enemies.add(new EnemyData(g, tipo, vel));
    }
 
    private void moverEnemigos(float tpf) {
        for (EnemyData e : enemies) {
            Vector3f ePos = e.geo.getLocalTranslation();
            Vector3f objetivo = null;
            float distMin = Float.MAX_VALUE;
            if (!p1Muerto) {
                float d = ePos.distance(p1Geo.getLocalTranslation());
                if (d < distMin) { distMin = d; objetivo = p1Geo.getLocalTranslation(); }
            }
            if (!p2Muerto) {
                float d = ePos.distance(p2Geo.getLocalTranslation());
                if (d < distMin) { objetivo = p2Geo.getLocalTranslation(); }
            }
            if (objetivo == null) continue;
            Vector3f dir = objetivo.subtract(ePos);
            if (dir.lengthSquared() <= 0.01f) continue;
            dir.normalizeLocal();
            Vector3f nPos = ePos.add(dir.mult(e.vel * tpf));
            nPos.y = 0.4f;
            e.geo.setLocalTranslation(nPos);
            if      (dir.x > 0) e.mirandoDerecha = true;
            else if (dir.x < 0) e.mirandoDerecha = false;
            e.animTimer += tpf;
            if (e.animTimer >= VEL_ANIM_ENEMY) {
                e.animTimer = 0f;
                e.frame = (e.frame + 1) % matEnemigoD[e.tipo].length;
                e.geo.setMaterial(e.mirandoDerecha
                        ? matEnemigoD[e.tipo][e.frame]
                        : matEnemigoI[e.tipo][e.frame]);
            }
        }
    }
 
    // ══════════════════════════════════════════════════════
    //  COLISIONES
    // ══════════════════════════════════════════════════════
 
    private void detectarColisiones() {
        List<BulletData> rb1 = new ArrayList<>(), rb2 = new ArrayList<>();
        List<EnemyData>  re1 = new ArrayList<>(),  re2 = new ArrayList<>();
 
        for (EnemyData e : enemies) {
            Vector3f ep = e.geo.getLocalTranslation();
            for (BulletData b : p1Bullets)
                if (b.geo.getLocalTranslation().distance(ep) < 0.55f) { rb1.add(b); re1.add(e); }
            for (BulletData b : p2Bullets)
                if (b.geo.getLocalTranslation().distance(ep) < 0.55f) { rb2.add(b); re2.add(e); }
        }
        for (BulletData b : rb1) { bulletsNode1.detachChild(b.geo); p1Bullets.remove(b); }
        for (BulletData b : rb2) { bulletsNode2.detachChild(b.geo); p2Bullets.remove(b); }
        for (EnemyData e : re1) { enemiesNode.detachChild(e.geo); enemies.remove(e); scoreP1 += PUNTOS_POR_ENEMIGO; score += PUNTOS_POR_ENEMIGO; }
        for (EnemyData e : re2) { if (!re1.contains(e)) { enemiesNode.detachChild(e.geo); enemies.remove(e); scoreP2 += PUNTOS_POR_ENEMIGO; score += PUNTOS_POR_ENEMIGO; } }
 
        // Contacto físico enemigo-jugador
        for (EnemyData e : enemies) {
            Vector3f ep = e.geo.getLocalTranslation();
            if (!p1Muerto && !p1Invencible && ep.distance(p1Geo.getLocalTranslation()) < 0.75f) recibirDanioP1();
            if (!p2Muerto && !p2Invencible && ep.distance(p2Geo.getLocalTranslation()) < 0.75f) recibirDanioP2();
        }
    }
 
    private void recibirDanioP1() {
        p1Vidas--;
        if (p1Vidas <= 0) {
            p1Muerto = true;
            p1Geo.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        } else {
            p1Invencible = true; p1InvTimer = INVENCIBILITY_TIME;
            p1ParpadeoTimer = PARPADEO_INTERVAL; p1Visible = true;
            p1Geo.setLocalTranslation(-3f, 0.3f, 0f);
        }
    }
 
    private void recibirDanioP2() {
        p2Vidas--;
        if (p2Vidas <= 0) {
            p2Muerto = true;
            p2Geo.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        } else {
            p2Invencible = true; p2InvTimer = INVENCIBILITY_TIME;
            p2ParpadeoTimer = PARPADEO_INTERVAL; p2Visible = true;
            p2Geo.setLocalTranslation(3f, 0.3f, 0f);
        }
    }
 
    private void activarGameOver() {
        gameOver = true;
        float W = cam.getWidth(), H = cam.getHeight();
        hudGameOver.setText("GAME OVER  |  P1: " + scoreP1 + "  P2: " + scoreP2 + "  Total: " + score);
        hudGameOver.setLocalTranslation((W - hudGameOver.getLineWidth()) / 2f, H / 2f + 60, 0);
        hudGameOver.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        hudReinicio.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
    }
 
    // ══════════════════════════════════════════════════════
    //  LIMPIEZA
    // ══════════════════════════════════════════════════════
 
    private void limpiarEntidades() {
        for (EnemyData  e : enemies)      enemiesNode.detachChild(e.geo);
        for (BulletData b : p1Bullets)    bulletsNode1.detachChild(b.geo);
        for (BulletData b : p2Bullets)    bulletsNode2.detachChild(b.geo);
        for (BulletData b : enemyBullets) enemyBulletsNode.detachChild(b.geo);
        enemies.clear(); p1Bullets.clear(); p2Bullets.clear(); enemyBullets.clear();
    }
 
    // ══════════════════════════════════════════════════════
    //  INPUT
    // ══════════════════════════════════════════════════════
 
    private void registrarInput() {
        inputManager.addMapping("INF_P1_LEFT",  new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("INF_P1_RIGHT", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("INF_P1_UP",    new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("INF_P1_DOWN",  new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("INF_P1_SHOOT", new KeyTrigger(KeyInput.KEY_RSHIFT));
        inputManager.addMapping("INF_P2_LEFT",  new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("INF_P2_RIGHT", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("INF_P2_UP",    new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("INF_P2_DOWN",  new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("INF_P2_SHOOT", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(inputListener,
                "INF_P1_LEFT","INF_P1_RIGHT","INF_P1_UP","INF_P1_DOWN","INF_P1_SHOOT",
                "INF_P2_LEFT","INF_P2_RIGHT","INF_P2_UP","INF_P2_DOWN","INF_P2_SHOOT");
    }
 
    private final ActionListener inputListener = (name, isPressed, tpf) -> {
        switch (name) {
            case "INF_P1_LEFT"  -> p1Left       = isPressed;
            case "INF_P1_RIGHT" -> p1Right      = isPressed;
            case "INF_P1_UP"    -> p1Up         = isPressed;
            case "INF_P1_DOWN"  -> p1Down       = isPressed;
            case "INF_P1_SHOOT" -> p1Disparando = isPressed;
            case "INF_P2_LEFT"  -> p2Left       = isPressed;
            case "INF_P2_RIGHT" -> p2Right      = isPressed;
            case "INF_P2_UP"    -> p2Up         = isPressed;
            case "INF_P2_DOWN"  -> p2Down       = isPressed;
            case "INF_P2_SHOOT" -> p2Disparando = isPressed;
        }
    };
 
    /** Recarga balas del Jugador 1 — tecla - */
    public void recargarP1() { if (!p1Muerto) p1Balas = 100; }
 
    /** Recarga balas del Jugador 2 — tecla R */
    public void recargarP2() { if (!p2Muerto) p2Balas = 100; }
 
    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════
 
    private float rnd(float edge) { return (FastMath.rand.nextFloat() * 2 - 1) * edge; }
 
    private Material matTex(String ruta) {
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setTexture("ColorMap", assetManager.loadTexture(ruta));
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return m;
    }
 
    private BitmapText htext(String t, float size, ColorRGBA color) {
        BitmapText bt = new BitmapText(guiFont, false);
        bt.setSize(size); bt.setColor(color); bt.setText(t);
        return bt;
    }
 
    // ── Clases internas ─────────────────────────────────────
 
    private static class BulletData {
        Geometry geo; Vector3f dir; float lifetime;
        BulletData(Geometry g, Vector3f d, float l) { geo = g; dir = d; lifetime = l; }
    }
 
    private static class EnemyData {
        Geometry geo; int tipo; float vel;
        int frame = 0; float animTimer = 0f; float shootTimer = 0f;
        boolean mirandoDerecha = true;
        EnemyData(Geometry g, int t, float v) { geo = g; tipo = t; vel = v; }
    }
}