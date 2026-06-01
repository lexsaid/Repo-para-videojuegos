package mygame;
 
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioData.DataType;
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
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
 
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
 
/**
 * =========================================================
 *  MODO BASE — lógica compartida por todos los modos
 * =========================================================
 *
 *  ModoClasico y ModoAbanico usan: Textures/Enemigo_1/
 *  ModoSupervivencia            usa: Textures/Enemigo_2/
 *  ModoZombies                  usa: Textures/Enemigo_2/
 *
 *  Cada subclase solo necesita sobreescribir:
 *    · rutaEnemigoDerecha()    → carpeta/prefijo derecha
 *    · rutaEnemigoIzquierda()  → carpeta/prefijo izquierda
 *    · cantidadSkinsEnemigo()  → cuántos frames tiene la animación
 *    · crearDisparoJugador()   → cuántas balas y en qué ángulos
 *    · colorArena() / colorEnemigo() / nombreModo()
 *    · y los valores de velocidad, vidas, spawn, etc.
 * =========================================================
 */
public abstract class ModoBase implements GameMode {
 
    // ── Constantes fijas ────────────────────────────────────
    protected static final float ARENA_SIZE         = 20f;
    protected static final float JUGADOR_MARGEN     = 0.35f;
    protected static final float BULLET_SPEED       = 14f;
    protected static final float BULLET_LIFETIME    = 3f;
    protected static final float ENEMY_BULLET_SPEED = 7f;
    protected static final float INVENCIBILITY_TIME = 2f;
    protected static final float PARPADEO_INTERVAL  = 0.15f;
    protected static final int   PUNTOS_POR_ENEMIGO = 100;
 
    // ── Recursos jME ────────────────────────────────────────
    protected Node         rootNode;
    protected Node         guiNode;
    protected AssetManager assetManager;
    protected InputManager inputManager;
    protected Camera       cam;
    protected BitmapFont   guiFont;
 
    // ── Nodos de escena ─────────────────────────────────────
    protected Node modoRoot;
    protected Node modoGui;
    protected Node enemiesNode;
    protected Node bulletsNode;
    protected Node enemyBulletsNode;
 
    // ── Jugador ─────────────────────────────────────────────
    protected Geometry player;
    protected Material playerMat;
    protected Vector3f playerDir = new Vector3f(1, 0, 0);
    protected float    shootTimer = 9999f;
 
    // ── Animación del jugador ────────────────────────────────
    private Texture[] animDerecha;
    private Texture[] animIzquierda;
    private String    ultimaDireccion = "DERECHA";
    private float     tiempoAnimJugador = 0f;
    private int       frameAnimJugador  = 0;
    private static final float VEL_ANIM_JUGADOR = 0.15f;
 
    // ── Animación de enemigos ────────────────────────────────
    private Material[] matsEnemigoDerecha;
    private Material[] matsEnemigoIzquierda;
    private static final float VEL_ANIM_ENEMIGO = 0.12f;
    
    // Texturas de balas
    private Material materialBalaJugador;
    private Material materialBalaEnemigo;
 
    // ── Vidas e invencibilidad ───────────────────────────────
    protected int     vidas           = 3;
    protected float   invencibleTimer = 0f;
    protected float   parpadeoTimer   = 0f;
    protected boolean estaInvencible  = false;
    protected boolean parpadeoVisible = true;
 
    // ── Estado ──────────────────────────────────────────────
    protected boolean gameOver      = false;
    protected int     scorePartida  = 0; // puntos de la partida actual (se resetea al reiniciar)
    protected int     scoreMaximo   = 0; // récord de la sesión (nunca baja)
 
    // ── HUD ─────────────────────────────────────────────────
    protected BitmapText hudVidas;
    protected BitmapText hudInfo;
    protected BitmapText hudCooldown;
    protected BitmapText hudScore;
    protected BitmapText hudScorePartida;
    protected BitmapText hudScoreMaximo;
    protected BitmapText hudGameOver;
    protected BitmapText hudReinicio;
    protected BitmapText hudBalas;
 
    // ── Música de fondo ─────────────────────────────────────
    protected AudioNode musicaFondo;
 
    // ── Entidades ───────────────────────────────────────────
    protected final List<BulletData> bullets      = new ArrayList<>();
    protected final List<BulletData> enemyBullets = new ArrayList<>();
    protected final List<EnemyData>  enemies      = new ArrayList<>();
 
    // ── Input ───────────────────────────────────────────────
    protected boolean moveLeft, moveRight, moveUp, moveDown;
    protected boolean disparando = false; // true mientras ESPACIO está presionado
    protected float   spawnTimer = 0f;
    private   String  inputPrefix = "";
 
    // ══════════════════════════════════════════════════════
    //  MÉTODOS QUE CADA SUBCLASE DEBE SOBREESCRIBIR
    // ══════════════════════════════════════════════════════
 
    protected abstract float    playerSpeed();
    protected abstract float    enemySpeed();
    protected abstract float    shootCooldown();
    protected abstract int      maxVidas();
    protected abstract int      maxEnemigos();
    protected abstract int      spawnInicial();
    protected abstract float    enemySpawnTime();
    protected abstract ColorRGBA colorArena();
    protected abstract ColorRGBA colorEnemigo();
    protected abstract String   nombreModo();
    protected abstract void     crearDisparoJugador();
 
    protected String rutaEnemigoDerecha()    { return "Textures/Enemigo_1/enemy_"; }
    protected String rutaEnemigoIzquierda()  { return "Textures/Enemigo_1/EnemyLeft_"; }
    protected int    cantidadSkinsEnemigo()  { return 5; }
    protected String rutaEscenario()         { return "Textures/Fondo/Escenario2.png"; }
    protected String rutaPersonajeDerecha()  { return "Textures/Personaje_1/PjRight_"; }
    protected String rutaPersonajeIzquierda(){ return "Textures/Personaje_1/PjLeft_"; }
    protected int    cantidadFramesPersonaje(){ return 5; }
 
    /** Frame desde el que empiezan los archivos del personaje. Por defecto 0. 
     *  Personaje_2 empieza en 1 → sobreescribir retornando 1. */
    protected int    frameInicialPersonaje()  { return 0; }
    protected void   actualizarHUDExtra(float tpf) {}
 
    /**
     * Ruta del archivo de música de fondo del modo.
     * Por defecto usa cancion5.wav (la misma del menú).
     * Sobreescribe en una subclase para usar una canción distinta,
     * o retorna null si ese modo no debe tener música propia.
     */
    protected String rutaMusica() { return "Sounds/cancion5.wav"; }
 
    protected void ocultarHUDBase() {
        hudVidas.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        hudInfo.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        hudCooldown.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        hudScore.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        hudScorePartida.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        hudScoreMaximo.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        hudBalas.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
    }
 
    protected boolean enemigosDisparan()  { return false; }

    /**
     * true  → mantener ESPACIO dispara automáticamente (respeta cooldown).
     * false → solo dispara al PRESIONAR ESPACIO (un toque = una bala).
     * Por defecto: false. Sobreescribir en modos ametralladora.
     */
    protected boolean disparoAutomatico() { return false; }
    protected float   enemyShootTime()   { return 2.5f; }
 
    @Override
    public int getBalasRestantes() { return -1; }
 
    // ══════════════════════════════════════════════════════
    //  CICLO DE VIDA
    // ══════════════════════════════════════════════════════
 
    @Override
    public void iniciar(Node rootNode, Node guiNode,
                        AssetManager assetManager,
                        InputManager inputManager,
                        Camera cam,
                        BitmapFont guiFont) {
 
        this.rootNode     = rootNode;
        this.guiNode      = guiNode;
        this.assetManager = assetManager;
        this.inputManager = inputManager;
        this.cam          = cam;
        this.guiFont      = guiFont;
 
        vidas          = maxVidas();
        estaInvencible = false;
        gameOver       = false;
        scorePartida  = 0;
        // scoreMaximo NO se resetea aquí: sobrevive a toda la sesión
        shootTimer    = shootCooldown();
        spawnTimer     = 0f;
        playerDir      = new Vector3f(1, 0, 0);
 
        modoRoot = new Node("modoRoot");
        modoGui  = new Node("modoGui");
        rootNode.attachChild(modoRoot);
        guiNode.attachChild(modoGui);
 
        cam.setLocation(new Vector3f(0, 20, 0));
        cam.lookAt(Vector3f.ZERO, new Vector3f(0, 0, -1));
 
        enemiesNode      = new Node("enemies");
        bulletsNode      = new Node("bullets");
        enemyBulletsNode = new Node("enemyBullets");
        modoRoot.attachChild(enemiesNode);
        modoRoot.attachChild(bulletsNode);
        modoRoot.attachChild(enemyBulletsNode);
 
        int frameInicial = frameInicialPersonaje();
        int totalFramesPersonaje = cantidadFramesPersonaje();
        animDerecha   = new Texture[totalFramesPersonaje];
        animIzquierda = new Texture[totalFramesPersonaje];
        for (int i = 0; i < totalFramesPersonaje; i++) {
            animDerecha[i]   = assetManager.loadTexture(rutaPersonajeDerecha()   + (i + frameInicial) + ".png");
            animIzquierda[i] = assetManager.loadTexture(rutaPersonajeIzquierda() + (i + frameInicial) + ".png");
        }
 
        int totalFrames = cantidadSkinsEnemigo();
        matsEnemigoDerecha   = new Material[totalFrames];
        matsEnemigoIzquierda = new Material[totalFrames];
        for (int i = 0; i < totalFrames; i++) {
            matsEnemigoDerecha[i]   = crearMaterialTextura(rutaEnemigoDerecha()   + i + ".png");
            matsEnemigoIzquierda[i] = crearMaterialTextura(rutaEnemigoIzquierda() + i + ".png");
        }
 
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
        setupJugador();
        setupHUD();
        registrarInput();
 
        for (int i = 0; i < spawnInicial(); i++) spawnEnemy();
 
        // ── Música de fondo ──────────────────────────────────
        String rutaAudio = rutaMusica();
        if (rutaAudio != null) {
            musicaFondo = new AudioNode(assetManager, rutaAudio, DataType.Buffer);
            musicaFondo.setLooping(true);
            musicaFondo.setVolume(0.5f);
            musicaFondo.setPositional(false);
            rootNode.attachChild(musicaFondo);
            musicaFondo.play();
        }
    }
 
    @Override
    public void update(float tpf) {
        if (gameOver) return;
 
        moverJugador(tpf);
        actualizarCamara();
        moverEnemigos(tpf);
        if (enemigosDisparan()) actualizarDisparoEnemigos(tpf);
        moverBalas(tpf);
        if (enemigosDisparan()) moverBalasEnemigas(tpf);
        detectarColisionesBalaEnemigo();
        detectarColisionEnemigoJugador();
        if (enemigosDisparan()) detectarColisionBalaEnemigoJugador();
 
        if (shootTimer < shootCooldown()) shootTimer += tpf;
        if (disparando && disparoAutomatico()) intentarDisparar();
 
        spawnTimer += tpf;
        if (spawnTimer >= enemySpawnTime() && enemies.size() < maxEnemigos()) {
            spawnEnemy();
            spawnTimer = 0f;
        }
 
        actualizarHUD();
        actualizarHUDExtra(tpf);
        actualizarAnimacionJugador(tpf);
    }
 
    @Override
    public void onReiniciar() {
        limpiarEntidades();
 
        vidas           = maxVidas();
        estaInvencible  = false;
        invencibleTimer = 0f;
        parpadeoTimer   = 0f;
        parpadeoVisible = true;
        gameOver        = false;
        spawnTimer      = 0f;
        shootTimer      = shootCooldown();
        playerDir       = new Vector3f(1, 0, 0);
 
        player.setLocalTranslation(0, 0.3f, 0);
        scorePartida = 0; // nueva partida → contador desde cero
 
        actualizarHUDVidas();
        hudGameOver.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        hudReinicio.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
 
        for (int i = 0; i < spawnInicial(); i++) spawnEnemy();
    }
 
    @Override
    public void destruir() {
        // ── Detener música de fondo ───────────────────────────
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
 
    @Override public int     getScore()      { return scoreMaximo; }
    @Override public boolean isGameOver()    { return gameOver; }
    @Override public String  getNombreModo() { return nombreModo(); }
 
    // ══════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DE ESCENA
    // ══════════════════════════════════════════════════════
 
    private void setupArena() {
        Box shape = new Box(ARENA_SIZE, 0.1f, ARENA_SIZE);
        Geometry arena = new Geometry("arena", shape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture texFondo = assetManager.loadTexture(rutaEscenario());
        texFondo.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("ColorMap", texFondo);
        arena.setMaterial(mat);
        arena.setLocalTranslation(0, -0.1f, 0);
        modoRoot.attachChild(arena);
    }
 
    private void setupJugador() {
        Box shape = new Box(0.8f, 0.01f, 1f);
        player    = new Geometry("player", shape);
        playerMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        playerMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        playerMat.setTexture("ColorMap", animDerecha[0]);
        player.setMaterial(playerMat);
        player.setQueueBucket(RenderQueue.Bucket.Transparent);
        player.rotate(0, 4.7f, 0);
        player.setLocalScale(1f, 1f, 1f);
        player.setLocalTranslation(0, 0.3f, 0);
        modoRoot.attachChild(player);
    }
 
    // ══════════════════════════════════════════════════════
    //  HUD
    // ══════════════════════════════════════════════════════
 
    private void setupHUD() {
        float W  = cam.getWidth();
        float H  = cam.getHeight();
        float fs = guiFont.getCharSet().getRenderedSize();
 
        hudVidas = htext("", fs * 1.5f, ColorRGBA.Red);
        hudVidas.setLocalTranslation(10, H - 10, 0);
        actualizarHUDVidas();
        modoGui.attachChild(hudVidas);
 
        hudInfo = htext(nombreModo() + "  |  WASD: mover  |  ESPACIO: disparar  |  ESC: menú",
                fs * 0.95f, new ColorRGBA(0.8f, 0.8f, 0.8f, 1f));
        hudInfo.setLocalTranslation(10, H - 38, 0);
        modoGui.attachChild(hudInfo);
 
        hudCooldown = htext("Disparo: [LISTO]", fs, ColorRGBA.Green);
        hudCooldown.setLocalTranslation(10, H - 58, 0);
        modoGui.attachChild(hudCooldown);
 
        hudScorePartida = htext("Puntos: 0", fs * 1.4f, ColorRGBA.Yellow);
        hudScorePartida.setLocalTranslation(W - 230, H - 10, 0);
        modoGui.attachChild(hudScorePartida);
 
        hudScoreMaximo = htext("Récord: 0", fs * 1.1f, new ColorRGBA(1f, 0.7f, 0f, 1f));
        hudScoreMaximo.setLocalTranslation(W - 230, H - 35, 0);
        modoGui.attachChild(hudScoreMaximo);
 
        // score (acumulado sesión) — solo para compatibilidad con modos que lo usen directamente
        hudScore = htext("", fs * 1.4f, ColorRGBA.Yellow); // oculto por defecto
        hudScore.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        modoGui.attachChild(hudScore);
 
        hudBalas = htext("", fs * 1.4f, ColorRGBA.Cyan);
        hudBalas.setLocalTranslation(W - 230, H - 40, 0);
        hudBalas.setCullHint(getBalasRestantes() == -1
                ? com.jme3.scene.Spatial.CullHint.Always
                : com.jme3.scene.Spatial.CullHint.Never);
        modoGui.attachChild(hudBalas);
 
        hudGameOver = htext("GAME OVER", fs * 3f, ColorRGBA.Red);
        hudGameOver.setLocalTranslation((W - hudGameOver.getLineWidth()) / 2f, H / 2f + 60, 0);
        hudGameOver.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        modoGui.attachChild(hudGameOver);
 
        hudReinicio = htext("R → Reiniciar   |   ESC → Volver al menú", fs * 1.1f, ColorRGBA.White);
        hudReinicio.setLocalTranslation((W - hudReinicio.getLineWidth()) / 2f, H / 2f + 15, 0);
        hudReinicio.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        modoGui.attachChild(hudReinicio);
    }
 
    private void actualizarHUDVidas() {
        int max = maxVidas();
        StringBuilder sb = new StringBuilder("Vidas: " + vidas + "  ");
        for (int i = 0; i < vidas; i++)  sb.append("♥ ");
        for (int i = vidas; i < max; i++) sb.append("♡ ");
        hudVidas.setText(sb.toString());
    }
 
    private void actualizarHUD() {
        if (shootTimer >= shootCooldown()) {
            hudCooldown.setColor(ColorRGBA.Green);
            hudCooldown.setText("Disparo: [LISTO]");
        } else {
            hudCooldown.setColor(ColorRGBA.Red);
            hudCooldown.setText(String.format("Disparo: %.1f s", shootCooldown() - shootTimer));
        }
        hudScorePartida.setText("Puntos: " + scorePartida);
        hudScoreMaximo.setText("Récord: " + scoreMaximo);
 
        if (getBalasRestantes() != -1) {
            int balas = getBalasRestantes();
            hudBalas.setText("Balas: " + balas);
            hudBalas.setColor(balas > 20 ? ColorRGBA.Cyan : ColorRGBA.Red);
        }
    }
 
    // ══════════════════════════════════════════════════════
    //  ANIMACIÓN DEL JUGADOR
    // ══════════════════════════════════════════════════════
 
    private void actualizarAnimacionJugador(float tpf) {
        boolean moviendose = moveLeft || moveRight || moveUp || moveDown;
 
        if (!moviendose) {
            if (ultimaDireccion.equals("IZQUIERDA")) {
                playerMat.setTexture("ColorMap", animIzquierda[0]);
            } else {
                playerMat.setTexture("ColorMap", animDerecha[0]);
            }
            playerMat.setColor("Color", ColorRGBA.White);
            frameAnimJugador  = 0;
            tiempoAnimJugador = 0f;
            return;
        }
 
        if      (moveRight) ultimaDireccion = "DERECHA";
        else if (moveLeft)  ultimaDireccion = "IZQUIERDA";
        else if (moveUp)    ultimaDireccion = "DERECHA";
        else                ultimaDireccion = "IZQUIERDA";
 
        tiempoAnimJugador += tpf;
        if (tiempoAnimJugador >= VEL_ANIM_JUGADOR) {
            tiempoAnimJugador = 0f;
            frameAnimJugador++;
            if (frameAnimJugador >= animDerecha.length) frameAnimJugador = 1;
 
            if (ultimaDireccion.equals("DERECHA")) {
                playerMat.setTexture("ColorMap", animDerecha[frameAnimJugador]);
            } else {
                playerMat.setTexture("ColorMap", animIzquierda[frameAnimJugador]);
            }
            playerMat.setColor("Color", ColorRGBA.White);
        }
    }
 
    // ══════════════════════════════════════════════════════
    //  MOVIMIENTO DEL JUGADOR
    // ══════════════════════════════════════════════════════
 
    private void moverJugador(float tpf) {
        Vector3f pos = player.getLocalTranslation().clone();
        Vector3f dir = new Vector3f();
 
        if (moveLeft)  dir.x -= 1;
        if (moveRight) dir.x += 1;
        if (moveUp)    dir.z -= 1;
        if (moveDown)  dir.z += 1;
 
        if (dir.lengthSquared() > 0) {
            dir.normalizeLocal();
            playerDir = dir.clone();
            pos.addLocal(dir.mult(playerSpeed() * tpf));
        }
 
        float lim = ARENA_SIZE - JUGADOR_MARGEN;
        pos.x = FastMath.clamp(pos.x, -lim, lim);
        pos.z = FastMath.clamp(pos.z, -lim, lim);
        pos.y = 0.3f;
        player.setLocalTranslation(pos);
        player.setLocalScale(1f, 1f, 1f); // evita encogimiento al cambiar textura
 
        if (estaInvencible) {
            invencibleTimer -= tpf;
            parpadeoTimer   -= tpf;
            if (parpadeoTimer <= 0) {
                parpadeoVisible = !parpadeoVisible;
                parpadeoTimer   = PARPADEO_INTERVAL;
                playerMat.setColor("Color", parpadeoVisible ? ColorRGBA.White : ColorRGBA.Blue);
            }
            if (invencibleTimer <= 0) {
                estaInvencible = false;
                playerMat.setColor("Color", ColorRGBA.White);
            }
        }
    }
 
    private void actualizarCamara() {
        Vector3f posJugador = player.getLocalTranslation();
        float margenCamara  = 7f;
        float limCamara     = Math.max(0, ARENA_SIZE - margenCamara);
        float camX = FastMath.clamp(posJugador.x, -limCamara, limCamara);
        float camZ = FastMath.clamp(posJugador.z, -limCamara, limCamara);
        cam.setLocation(new Vector3f(camX, 20, camZ));
    }
 
    // ══════════════════════════════════════════════════════
    //  IA Y ANIMACIÓN DE ENEMIGOS
    // ══════════════════════════════════════════════════════
 
    private void moverEnemigos(float tpf) {
        Vector3f pPos = player.getLocalTranslation();
 
        for (EnemyData e : enemies) {
            Vector3f posActual = e.geometry.getLocalTranslation();
            Vector3f dir       = pPos.subtract(posActual);
 
            if (dir.lengthSquared() <= 0.01f) continue;
 
            dir.normalizeLocal();
            Vector3f nuevaPos = posActual.add(dir.mult(enemySpeed() * tpf));
            nuevaPos.y = 0.4f;
            e.geometry.setLocalTranslation(nuevaPos);
 
            if      (dir.x > 0) e.mirandoDerecha = true;
            else if (dir.x < 0) e.mirandoDerecha = false;
 
            e.tiempoAnimacion += tpf;
            if (e.tiempoAnimacion >= VEL_ANIM_ENEMIGO) {
                e.tiempoAnimacion = 0f;
                e.frameActual++;
                if (e.frameActual >= matsEnemigoDerecha.length) e.frameActual = 0;
 
                e.geometry.setMaterial(
                    e.mirandoDerecha
                        ? matsEnemigoDerecha[e.frameActual]
                        : matsEnemigoIzquierda[e.frameActual]
                );
            }
        }
    }
 
    // ══════════════════════════════════════════════════════
    //  DISPARO DE ENEMIGOS (solo ModoSupervivencia)
    // ══════════════════════════════════════════════════════
 
    private void actualizarDisparoEnemigos(float tpf) {
        Vector3f pPos = player.getLocalTranslation();
        for (EnemyData e : enemies) {
            e.shootTimer += tpf;
            if (e.shootTimer >= enemyShootTime()) {
                e.shootTimer = 0f;
                Vector3f dir = pPos.subtract(e.geometry.getLocalTranslation());
                if (dir.lengthSquared() > 0.01f) {
                    dir.normalizeLocal();
                    crearBalaEnemiga(e.geometry.getLocalTranslation().clone(), dir);
                }
            }
        }
    }
 
    protected void crearBalaEnemiga(Vector3f origen, Vector3f dir) {
        Quad shape = new Quad(0.7f, 0.35f);
        Geometry g = new Geometry("eBullet", shape);
        g.center();
        orientarBala(g, dir.normalize());
        Material matE = materialBalaEnemigo.clone();
        matE.getAdditionalRenderState().setBlendMode(
            com.jme3.material.RenderState.BlendMode.Alpha);
        g.setMaterial(matE);
        g.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
        g.setLocalTranslation(origen);
        enemyBulletsNode.attachChild(g);
        enemyBullets.add(new BulletData(g, dir, BULLET_LIFETIME));
    }

    /**
     * Rota un Quad plano (en el plano XZ, vista top-down) para que
     * su eje largo apunte en la dirección de vuelo.
     * El Quad por defecto está en el plano XY, así que primero lo
     * tumbamos 90° en X y luego lo giramos en Y según la dirección.
     */
    private void orientarBala(Geometry g, Vector3f dir) {
        // Ángulo entre el eje +X y la dirección del proyectil (en el plano XZ)
        float angulo = FastMath.atan2(dir.z, dir.x);
        Quaternion rot = new Quaternion();
        // -90° en X  → tumba el quad al suelo (plano XZ)
        // angulo en Y → apunta hacia la dirección correcta
        Quaternion tumbado = new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
        Quaternion giro    = new Quaternion().fromAngleAxis(-angulo, Vector3f.UNIT_Y);
        g.setLocalRotation(giro.mult(tumbado));
        g.setLocalTranslation(0, 0.25f, 0); // pequeña elevación para que no se hunda en el suelo
    }
 
    // ══════════════════════════════════════════════════════
    //  BALAS DEL JUGADOR
    // ══════════════════════════════════════════════════════
 
    protected void intentarDisparar() {
        if (gameOver || shootTimer < shootCooldown()) return;
        shootTimer = 0f;
        crearDisparoJugador();
    }
 
    protected void crearBalaJugador(Vector3f dir) {
        Quad shape = new Quad(0.7f, 0.35f);
        Geometry g = new Geometry("bullet", shape);
        g.center();
        orientarBala(g, dir.normalize());

        Material mat = materialBalaJugador.clone();
        mat.getAdditionalRenderState().setBlendMode(
            com.jme3.material.RenderState.BlendMode.Alpha);
        g.setMaterial(mat);
        g.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);

        // Posición: salir desde el jugador, misma altura que el sprite
        Vector3f pos = player.getLocalTranslation().clone();
        pos.y = 0.25f;
        g.setLocalTranslation(pos);
        bulletsNode.attachChild(g);
        bullets.add(new BulletData(g, dir.normalizeLocal(), BULLET_LIFETIME));
    }
 
    protected void crearBalaEnAngulo(float gradosY) {
        Quaternion rot = new Quaternion();
        rot.fromAngleAxis(FastMath.DEG_TO_RAD * gradosY, Vector3f.UNIT_Y);
        crearBalaJugador(rot.mult(playerDir));
    }
 
    private void moverBalas(float tpf) {
        Iterator<BulletData> it = bullets.iterator();
        while (it.hasNext()) {
            BulletData b = it.next();
            b.lifetime -= tpf;
            if (b.lifetime <= 0) { bulletsNode.detachChild(b.geometry); it.remove(); continue; }
            Vector3f p = b.geometry.getLocalTranslation().add(b.direction.mult(BULLET_SPEED * tpf));
            p.y = 0.25f; // altura fija (sprite top-down)
            b.geometry.setLocalTranslation(p);
            if (Math.abs(p.x) > ARENA_SIZE + 1 || Math.abs(p.z) > ARENA_SIZE + 1) {
                bulletsNode.detachChild(b.geometry); it.remove();
            }
        }
    }
 
    private void moverBalasEnemigas(float tpf) {
        Iterator<BulletData> it = enemyBullets.iterator();
        while (it.hasNext()) {
            BulletData b = it.next();
            b.lifetime -= tpf;
            if (b.lifetime <= 0) { enemyBulletsNode.detachChild(b.geometry); it.remove(); continue; }
            Vector3f p = b.geometry.getLocalTranslation().add(b.direction.mult(ENEMY_BULLET_SPEED * tpf));
            p.y = 0.25f;
            b.geometry.setLocalTranslation(p);
            if (Math.abs(p.x) > ARENA_SIZE + 1 || Math.abs(p.z) > ARENA_SIZE + 1) {
                enemyBulletsNode.detachChild(b.geometry); it.remove();
            }
        }
    }
 
    // ══════════════════════════════════════════════════════
    //  COLISIONES
    // ══════════════════════════════════════════════════════
 
    private void detectarColisionesBalaEnemigo() {
        List<BulletData> rb = new ArrayList<>();
        List<EnemyData>  re = new ArrayList<>();
        for (BulletData b : bullets)
            for (EnemyData e : enemies)
                if (b.geometry.getLocalTranslation().distance(e.geometry.getLocalTranslation()) < 0.55f) {
                    rb.add(b); re.add(e);
                }
        for (BulletData b : rb) { bulletsNode.detachChild(b.geometry);  bullets.remove(b); }
        for (EnemyData  e : re) {
            enemiesNode.detachChild(e.geometry);
            enemies.remove(e);
            scorePartida += PUNTOS_POR_ENEMIGO;
            if (scorePartida > scoreMaximo) scoreMaximo = scorePartida;
        }
    }
 
    private void detectarColisionEnemigoJugador() {
        if (estaInvencible) return;
        Vector3f pp = player.getLocalTranslation();
        for (EnemyData e : enemies)
            if (pp.distance(e.geometry.getLocalTranslation()) < 0.75f) {
                recibirDanio(); return;
            }
    }
 
    private void detectarColisionBalaEnemigoJugador() {
        if (gameOver || estaInvencible) return;
        Vector3f pp = player.getLocalTranslation();
        List<BulletData> rb = new ArrayList<>();
        for (BulletData b : enemyBullets)
            if (pp.distance(b.geometry.getLocalTranslation()) < 0.45f) {
                rb.add(b); recibirDanio(); break;
            }
        for (BulletData b : rb) { enemyBulletsNode.detachChild(b.geometry); enemyBullets.remove(b); }
    }
 
    // ══════════════════════════════════════════════════════
    //  VIDA / GAME OVER
    // ══════════════════════════════════════════════════════
 
    protected void recibirDanio() {
        vidas--;
        actualizarHUDVidas();
        if (vidas <= 0) { activarGameOver(); return; }
        if (maxVidas() > 1) {
            estaInvencible  = true;
            invencibleTimer = INVENCIBILITY_TIME;
            parpadeoTimer   = PARPADEO_INTERVAL;
            parpadeoVisible = true;
        }
        player.setLocalTranslation(0, 0.3f, 0);
    }
 
    protected void activarGameOver() {
        if (gameOver) return;
        gameOver = true;
        playerMat.setColor("Color", ColorRGBA.Gray);
        hudGameOver.setText("GAME OVER  —  " + scorePartida + " pts  |  Récord: " + scoreMaximo);
        float W = cam.getWidth();
        hudGameOver.setLocalTranslation((W - hudGameOver.getLineWidth()) / 2f, cam.getHeight() / 2f + 60, 0);
        hudGameOver.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        hudReinicio.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
    }
 
    // ══════════════════════════════════════════════════════
    //  SPAWN DE ENEMIGOS
    // ══════════════════════════════════════════════════════
 
    protected void spawnEnemy() {
        Box shape = new Box(0.8f, 0f, 1f);
        Geometry g = new Geometry("enemy", shape);
        g.setMaterial(matsEnemigoDerecha[0]);
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
        enemies.add(new EnemyData(g, FastMath.rand.nextFloat() * enemyShootTime()));
    }
 
    private float rnd(float edge) {
        return (FastMath.rand.nextFloat() * 2 - 1) * edge;
    }
 
    // ══════════════════════════════════════════════════════
    //  LIMPIEZA
    // ══════════════════════════════════════════════════════
 
    private void limpiarEntidades() {
        for (EnemyData  e : enemies)      enemiesNode.detachChild(e.geometry);
        for (BulletData b : bullets)      bulletsNode.detachChild(b.geometry);
        for (BulletData b : enemyBullets) enemyBulletsNode.detachChild(b.geometry);
        enemies.clear(); bullets.clear(); enemyBullets.clear();
    }
 
    // ══════════════════════════════════════════════════════
    //  INPUT
    // ══════════════════════════════════════════════════════
 
    private void registrarInput() {
        String pfx = nombreModo().replace(" ", "");
        inputManager.addMapping(pfx + "LEFT",  new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(pfx + "RIGHT", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(pfx + "UP",    new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(pfx + "DOWN",  new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(pfx + "SHOOT", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(inputListener,
                pfx + "LEFT", pfx + "RIGHT", pfx + "UP", pfx + "DOWN", pfx + "SHOOT");
        this.inputPrefix = pfx;
    }
 
    private final ActionListener inputListener = (name, isPressed, tpf) -> {
        String pfx = inputPrefix;
        if (name.equals(pfx + "LEFT"))  moveLeft   = isPressed;
        if (name.equals(pfx + "RIGHT")) moveRight  = isPressed;
        if (name.equals(pfx + "UP"))    moveUp     = isPressed;
        if (name.equals(pfx + "DOWN"))  moveDown   = isPressed;
        if (name.equals(pfx + "SHOOT")) {
            disparando = isPressed;
            if (isPressed && !disparoAutomatico()) intentarDisparar();
        }
    };
 
    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════
 
    private Material crearMaterialTextura(String ruta) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture(ruta));
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return mat;
    }
 
    private BitmapText htext(String t, float size, ColorRGBA color) {
        BitmapText bt = new BitmapText(guiFont, false);
        bt.setSize(size);
        bt.setColor(color);
        bt.setText(t);
        return bt;
    }
 
    // ── Clases internas ─────────────────────────────────────
 
    protected static class BulletData {
        Geometry geometry; Vector3f direction; float lifetime;
        BulletData(Geometry g, Vector3f d, float l) { geometry = g; direction = d; lifetime = l; }
    }
 
    protected static class EnemyData {
        Geometry geometry;
        float    shootTimer;
        int      frameActual     = 0;
        float    tiempoAnimacion = 0f;
        boolean  mirandoDerecha  = true;
        EnemyData(Geometry g, float st) { geometry = g; shootTimer = st; }
    }
}