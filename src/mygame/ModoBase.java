package mygame;

import com.jme3.asset.AssetManager;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * =========================================================
 *  MODO BASE — lógica compartida por todos los modos
 * =========================================================
 *
 *  Los tres modos (ModoClasico, ModoAbanico, ModoSupervivencia)
 *  extienden esta clase. Solo sobreescriben:
 *    · configurar()        → constantes del modo
 *    · crearDisparoJugador() → cuántas balas y en qué ángulos
 *    · modoNombre()        → nombre para el HUD/menú
 *
 *  MECÁNICAS COMUNES:
 *  - Jugador limitado dentro del mapa (ARENA_SIZE)
 *  - Spawn inicial doble + spawn periódico (MAX_ENEMIES = 20)
 *  - Cooldown de disparo configurable
 *  - Colisiones bala↔enemigo, enemigo↔jugador
 *  - Puntuación: 100 pts por enemigo eliminado
 *  - HUD: vidas, cooldown, puntuación
 *  - Game Over con puntuación total
 *  - R → reiniciar, ESC → el Main captura el evento
 *
 *  PUNTUACIÓN:
 *  - 100 pts por cada enemigo eliminado
 *  - Se acumula durante toda la sesión (sobrevive al reinicio)
 *  - Al volver al menú se muestra el total de la sesión
 * =========================================================
 */
public abstract class ModoBase implements GameMode {

    // ── Constantes fijas para todos los modos ───────────────
    protected static final float ARENA_SIZE        = 20f;
    protected static final float JUGADOR_MARGEN    = 0.35f;
    protected static final float BULLET_SPEED      = 14f;
    protected static final float BULLET_LIFETIME   = 3f;
    protected static final float ENEMY_BULLET_SPEED = 7f;
    protected static final float INVENCIBILITY_TIME = 2f;
    protected static final float PARPADEO_INTERVAL  = 0.15f;
    protected static final int   PUNTOS_POR_ENEMIGO = 100;

    // ── Recursos jME compartidos ────────────────────────────
    protected Node         rootNode;
    protected Node         guiNode;
    protected AssetManager assetManager;
    protected InputManager inputManager;
    protected Camera       cam;
    protected BitmapFont   guiFont;

    // ── Nodos de escena ─────────────────────────────────────
    protected Node modoRoot;          // nodo raíz del modo (fácil de limpiar)
    protected Node modoGui;           // nodo GUI del modo
    protected Node enemiesNode;
    protected Node bulletsNode;
    protected Node enemyBulletsNode;

    // ── Jugador ─────────────────────────────────────────────
    protected Geometry player;
    protected Material playerMat;
    protected Vector3f playerDir  = new Vector3f(1, 0, 0);
    protected float    shootTimer = 9999f; // listo para disparar al inicio
    // ── Variables para la animación ─────────────────────────
    private Texture[] animDerecha = new Texture[4];
    private float tiempoAnimacion = 0f;
    private int frameActual = 0;
    private final float VEL_ANIMACION = 0.15f; // Cambia de imagen cada 0.15 segundos
    
    private Texture[] animIzquierda = new Texture[4];
    
    
    // ── Vidas e invencibilidad ───────────────────────────────
    protected int     vidas           = 3;
    protected float   invencibleTimer = 0f;
    protected float   parpadeoTimer   = 0f;
    protected boolean estaInvencible  = false;
    protected boolean parpadeoVisible = true;

    // ── Estado ──────────────────────────────────────────────
    protected boolean gameOver = false;
    protected int     score    = 0;   // puntos de esta sesión

    // ── HUD ─────────────────────────────────────────────────
    protected BitmapText hudVidas;
    protected BitmapText hudInfo;
    protected BitmapText hudCooldown;
    protected BitmapText hudScore;
    protected BitmapText hudGameOver;
    protected BitmapText hudReinicio;

    // ── Entidades ───────────────────────────────────────────
    protected final List<BulletData> bullets      = new ArrayList<>();
    protected final List<BulletData> enemyBullets = new ArrayList<>();
    protected final List<EnemyData>  enemies      = new ArrayList<>();

    // ── Input ───────────────────────────────────────────────
    protected boolean moveLeft, moveRight, moveUp, moveDown;
    protected float   spawnTimer = 0f;

    // ══════════════════════════════════════════════════════
    //  CONFIGURACIÓN — cada subclase sobreescribe esto
    // ══════════════════════════════════════════════════════

    /** Velocidad del jugador */
    protected abstract float playerSpeed();

    /** Velocidad de los enemigos */
    protected abstract float enemySpeed();

    /** Cooldown entre salvas del jugador (segundos) */
    protected abstract float shootCooldown();

    /** Máximo de vidas del jugador */
    protected abstract int   maxVidas();

    /** Máximo de enemigos en pantalla */
    protected abstract int   maxEnemigos();

    /** Cuántos enemigos spawnan al inicio */
    protected abstract int   spawnInicial();

    /** Segundos entre spawns de enemigos */
    protected abstract float enemySpawnTime();

    /** Color del suelo para diferenciar modos visualmente */
    protected abstract ColorRGBA colorArena();

    /** Color de los enemigos */
    protected abstract ColorRGBA colorEnemigo();

    /**
     * Genera las balas del jugador. La subclase llama a
     * {@link #crearBalaJugador(Vector3f)} con cada dirección.
     * Solo se llama cuando el cooldown ya pasó.
     */
    protected abstract void crearDisparoJugador();

    /**
     * True si los enemigos también disparan.
     * Sobreescribe en ModoSupervivencia.
     */
    protected boolean enemigosDisparan() { return false; }

    /** Segundos entre disparos de cada enemigo */
    protected float enemyShootTime()     { return 2.5f; }

    // ══════════════════════════════════════════════════════
    //  INTERFACE GameMode — CICLO DE VIDA
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

        // Restablecer estado
        vidas           = maxVidas();
        estaInvencible  = false;
        gameOver        = false;
        score           = 0;
        shootTimer      = shootCooldown(); // listo desde el inicio
        spawnTimer      = 0f;
        playerDir       = new Vector3f(1, 0, 0);

        // Nodos propios del modo (fácil de destruir luego)
        modoRoot = new Node("modoRoot");
        modoGui  = new Node("modoGui");
        rootNode.attachChild(modoRoot);
        guiNode.attachChild(modoGui);
        
        // Cargar las texturas de la derecha
        
        animDerecha[0] = assetManager.loadTexture("Textures/Personaje_1/PjDer1.png"); // Caminando 1
        animDerecha[1] = assetManager.loadTexture("Textures/Personaje_1/PjDer2.png"); // Caminando 2
        animDerecha[2] = assetManager.loadTexture("Textures/Personaje_1/PjDer3.png"); // Caminando 3
        animDerecha[3] = assetManager.loadTexture("Textures/Personaje_1/PjDer4.png"); // Caminando 4
       
        animIzquierda[0] = assetManager.loadTexture("Textures/Personaje_1/l0_sprite_1.png"); // Caminando 1
        animIzquierda[1] = assetManager.loadTexture("Textures/Personaje_1/l0_sprite_2.png"); // Caminando 2
        animIzquierda[2] = assetManager.loadTexture("Textures/Personaje_1/l0_sprite_3.png"); // Caminando 3
        animIzquierda[3] = assetManager.loadTexture("Textures/Personaje_1/l0_sprite_4.png"); // Caminando 4
        
        
        enemiesNode      = new Node("enemies");
        bulletsNode      = new Node("bullets");
        enemyBulletsNode = new Node("enemyBullets");
        modoRoot.attachChild(enemiesNode);
        modoRoot.attachChild(bulletsNode);
        modoRoot.attachChild(enemyBulletsNode);

        // Reposicionar cámara (puede estar en otra posición si venía del menú)
        cam.setLocation(new Vector3f(0, 20, 0));
        cam.lookAt(Vector3f.ZERO, new Vector3f(0, 0, -1));

        setupArena();
        setupJugador();
        setupHUD();
        registrarInput();

        for (int i = 0; i < spawnInicial(); i++) spawnEnemy();
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

        // Cooldown disparo
        if (shootTimer < shootCooldown()) shootTimer += tpf;

        // Spawn periódico
        spawnTimer += tpf;
        if (spawnTimer >= enemySpawnTime() && enemies.size() < maxEnemigos()) {
            spawnEnemy();
            spawnTimer = 0f;
        }
        
        actualizarHUD();
        
        Material matJugador = player.getMaterial();
        if (moveRight) {
            // Le sumamos el tiempo que pasó desde el último frame
            tiempoAnimacion += tpf; 
            
            // Si ya pasaron 0.15 segundos...
            if (tiempoAnimacion > VEL_ANIMACION) {
                tiempoAnimacion = 0f; // Reiniciamos el reloj
                frameActual++;        // Pasamos al siguiente dibujo
                
                // Si ya pasamos el dibujo 4, regresamos al 1 
                // (Brincamos el 0 porque ese es solo para estar quieto)
                if (frameActual >= animDerecha.length) {
                    frameActual = 1; 
                }
                
                // Le ponemos la nueva imagen al mono
                matJugador.setTexture("ColorMap", animDerecha[frameActual]);
            }
            
        }else if (moveLeft){
            // Le sumamos el tiempo que pasó desde el último frame
            tiempoAnimacion += tpf; 
            
            // Si ya pasaron 0.15 segundos...
            if (tiempoAnimacion > VEL_ANIMACION) {
                tiempoAnimacion = 0f; // Reiniciamos el reloj
                frameActual++;        // Pasamos al siguiente dibujo
                
                // Si ya pasamos el dibujo 4, regresamos al 1 
                // (Brincamos el 0 porque ese es solo para estar quieto)
                if (frameActual >= animIzquierda.length) {
                    frameActual = 1; 
                }
                
                // Le ponemos la nueva imagen al mono
                matJugador.setTexture("ColorMap", animIzquierda[frameActual]);
            }
            
        }else if (moveDown){
            // Le sumamos el tiempo que pasó desde el último frame
            tiempoAnimacion += tpf; 
            
            // Si ya pasaron 0.15 segundos...
            if (tiempoAnimacion > VEL_ANIMACION) {
                tiempoAnimacion = 0f; // Reiniciamos el reloj
                frameActual++;        // Pasamos al siguiente dibujo
                
                // Si ya pasamos el dibujo 4, regresamos al 1 
                // (Brincamos el 0 porque ese es solo para estar quieto)
                if (frameActual >= animIzquierda.length) {
                    frameActual = 1; 
                }
                
                // Le ponemos la nueva imagen al mono
                matJugador.setTexture("ColorMap", animIzquierda[frameActual]);
            }
        }else if (moveUp){
            // Le sumamos el tiempo que pasó desde el último frame
            tiempoAnimacion += tpf; 
            
            // Si ya pasaron 0.15 segundos...
            if (tiempoAnimacion > VEL_ANIMACION) {
                tiempoAnimacion = 0f; // Reiniciamos el reloj
                frameActual++;        // Pasamos al siguiente dibujo
                
                // Si ya pasamos el dibujo 4, regresamos al 1 
                // (Brincamos el 0 porque ese es solo para estar quieto)
                if (frameActual >= animDerecha.length) {
                    frameActual = 1; 
                }
                
                // Le ponemos la nueva imagen al mono
                matJugador.setTexture("ColorMap", animDerecha[frameActual]);
            }
            
        }else if (!moveLeft && !moveUp && !moveDown && !moveRight) {
            Texture textPerson1 = assetManager.loadTexture("Textures/Personaje_1/Personaje_1.png"); //textura 
            // Si NO se está moviendo a ningún lado, lo dejamos quieto y reiniciamos todo
            matJugador.setTexture("ColorMap", textPerson1); // PjDer0.png
            frameActual = 0;
            tiempoAnimacion = 0f;
        }
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
        playerMat.setColor("Color", ColorRGBA.White);
        player.setLocalTranslation(0, 0.3f, 0);
        // score NO se resetea: se acumula en la sesión

        actualizarHUDVidas();
        hudGameOver.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        hudReinicio.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        
        vidas = maxVidas();
        
        for (int i = 0; i < spawnInicial(); i++) spawnEnemy();
    }

    @Override
    public void destruir() {
        limpiarEntidades();
        modoRoot.removeFromParent();
        modoGui.removeFromParent();
        // Quitar listeners de input registrados por este modo
        try { inputManager.removeListener(inputListener); } catch (Exception ignored) {}
    }

    @Override
    public int     getScore()      { return score; }
    @Override
    public boolean isGameOver()    { return gameOver; }
    @Override
    public String  getNombreModo() { return nombreModo(); }

    /** Nombre legible del modo — sobreescribir en cada subclase */
    protected abstract String nombreModo();

    // ══════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DE ESCENA
    // ══════════════════════════════════════════════════════
    private void setupArena() {
        Box shape = new Box(ARENA_SIZE, 0.1f, ARENA_SIZE);
        Geometry arena = new Geometry("arena", shape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", colorArena());
        arena.setMaterial(mat);
        arena.setLocalTranslation(0, -0.1f, 0);
        modoRoot.attachChild(arena);
    }

    private void setupJugador() {
        Texture textPerson1 = assetManager.loadTexture("Textures/Personaje_1/Personaje_1.png"); //textura 
        Box shape = new Box(0.8f, 0f, 1f);
        player = new Geometry("player", shape);
        playerMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        player.rotate(0, 4.7f, 0);
        //playerMat.setColor("Color", ColorRGBA.Blue);
        playerMat.setTexture("ColorMap", textPerson1);
        playerMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        player.setMaterial(playerMat);
        player.setQueueBucket(RenderQueue.Bucket.Transparent);
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

        hudInfo = htext(nombreModo() + infoExtra(), fs * 0.95f,
                new ColorRGBA(0.8f, 0.8f, 0.8f, 1f));
        hudInfo.setLocalTranslation(10, H - 38, 0);
        modoGui.attachChild(hudInfo);

        hudCooldown = htext("Disparo: [LISTO]", fs, ColorRGBA.Green);
        hudCooldown.setLocalTranslation(10, H - 58, 0);
        modoGui.attachChild(hudCooldown);

        // Puntuación — esquina superior derecha
        hudScore = htext("Puntos: 0", fs * 1.4f, ColorRGBA.Yellow);
        hudScore.setLocalTranslation(W - 230, H - 10, 0);
        modoGui.attachChild(hudScore);

        // Game Over
        hudGameOver = htext("GAME OVER", fs * 3f, ColorRGBA.Red);
        hudGameOver.setLocalTranslation(
                (W - hudGameOver.getLineWidth()) / 2f, H / 2f + 60, 0);
        hudGameOver.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        modoGui.attachChild(hudGameOver);

        hudReinicio = htext("R → Reiniciar   |   ESC → Volver al menú", fs * 1.1f, ColorRGBA.White);
        hudReinicio.setLocalTranslation(
                (W - hudReinicio.getLineWidth()) / 2f, H / 2f + 15, 0);
        hudReinicio.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        modoGui.attachChild(hudReinicio);
    }

    /** Línea de info extra que cada subclase puede customizar */
    protected String infoExtra() { return "  |  WASD: mover  |  ESPACIO: disparar  |  ESC: menú"; }

    private void actualizarHUDVidas() {
        int max = maxVidas();
        StringBuilder sb = new StringBuilder("Vidas: " + vidas);
        for (int i = 0; i < vidas; i++)   sb.append("♥ ");
        for (int i = vidas; i < max; i++)  sb.append("♡ ");
        hudVidas.setText(sb.toString());
    }

    private void actualizarHUD() {
        // Cooldown
        if (shootTimer >= shootCooldown()) {
            hudCooldown.setColor(ColorRGBA.Green);
            hudCooldown.setText("Disparo: [LISTO]");
        } else {
            hudCooldown.setColor(ColorRGBA.Red);
            hudCooldown.setText(String.format("Disparo: %.1f s", shootCooldown() - shootTimer));
        }
        // Puntuación
        hudScore.setText("Puntos: " + score);
    }

    // ══════════════════════════════════════════════════════
    //  MOVIMIENTO DEL JUGADOR + límite de mapa + parpadeo
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

        // Límite estricto del mapa — el jugador NUNCA sale
        float lim = ARENA_SIZE - JUGADOR_MARGEN;
        pos.x = FastMath.clamp(pos.x, -lim, lim);
        pos.z = FastMath.clamp(pos.z, -lim, lim);
        pos.y = 0.3f;
        player.setLocalTranslation(pos);

        // Parpadeo de invencibilidad
        if (estaInvencible) {
            invencibleTimer -= tpf;
            parpadeoTimer   -= tpf;
            if (parpadeoTimer <= 0) {
                parpadeoVisible = !parpadeoVisible;
                parpadeoTimer   = PARPADEO_INTERVAL;
                playerMat.setColor("Color",
                        parpadeoVisible ? ColorRGBA.White : ColorRGBA.Blue);
            }
            if (invencibleTimer <= 0) {
                estaInvencible = false;
                playerMat.setColor("Color", ColorRGBA.White);
            }
        }
    }
    
    private void actualizarCamara() {
        // 1. Obtenemos la posición actual de nuestro jugador
        Vector3f posJugador = player.getLocalTranslation();
        
        // 2. Definimos un "margen visual". Como la cámara está a Y=20 de altura,
        // alcanza a ver mucha distancia. Necesitamos que se detenga unos 15 metros 
        // antes de llegar al límite real del mapa.
        float margenVisualCamara = 7f; // Puedes subir o bajar este número si se ve lo negro
        float limCamara = ARENA_SIZE - margenVisualCamara;
        
        // Protección matemática por si el mapa es muy pequeño
        if (limCamara < 0) limCamara = 0; 
        
        // 3. Forzamos a la cámara a no rebasar ese límite
        float camX = FastMath.clamp(posJugador.x, -limCamara, limCamara);
        float camZ = FastMath.clamp(posJugador.z, -limCamara, limCamara);
        
        // 4. Actualizamos la posición de la cámara respetando su altura de 20
        cam.setLocation(new Vector3f(camX, 20, camZ));
    }

    // ══════════════════════════════════════════════════════
    //  IA ENEMIGOS
    // ══════════════════════════════════════════════════════
    private void moverEnemigos(float tpf) {
        Vector3f pPos = player.getLocalTranslation();
        for (EnemyData e : enemies) {
            Vector3f dir = pPos.subtract(e.geometry.getLocalTranslation());
            if (dir.lengthSquared() > 0.01f) {
                dir.normalizeLocal();
                Vector3f np = e.geometry.getLocalTranslation()
                        .add(dir.mult(enemySpeed() * tpf));
                np.y = 0.4f;
                e.geometry.setLocalTranslation(np);
            }
        }
    }

    // Disparo de enemigos (solo si enemigosDisparan() = true)
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
        Box shape = new Box(0.12f, 0.12f, 0.12f);
        Geometry g = new Geometry("eBullet", shape);
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", ColorRGBA.Magenta);
        g.setMaterial(m);
        g.setLocalTranslation(origen);
        enemyBulletsNode.attachChild(g);
        enemyBullets.add(new BulletData(g, dir, BULLET_LIFETIME));
    }

    // ══════════════════════════════════════════════════════
    //  BALAS DEL JUGADOR
    // ══════════════════════════════════════════════════════
    protected void intentarDisparar() {
        if (gameOver || shootTimer < shootCooldown()) return;
        shootTimer = 0f;
        crearDisparoJugador(); // delegado a la subclase
    }

    /** Helper que usan las subclases para crear una bala en la dirección dada */
    protected void crearBalaJugador(Vector3f dir) {
        Box shape = new Box(0.1f, 0.1f, 0.1f);
        Geometry g = new Geometry("bullet", shape);
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", ColorRGBA.Yellow);
        g.setMaterial(m);
        g.setLocalTranslation(player.getLocalTranslation().clone());
        bulletsNode.attachChild(g);
        bullets.add(new BulletData(g, dir.normalizeLocal(), BULLET_LIFETIME));
    }

    /** Helper para crear bala rotada N grados respecto a playerDir */
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
            Vector3f p = b.geometry.getLocalTranslation()
                    .add(b.direction.mult(BULLET_SPEED * tpf));
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
            Vector3f p = b.geometry.getLocalTranslation()
                    .add(b.direction.mult(ENEMY_BULLET_SPEED * tpf));
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
                if (b.geometry.getLocalTranslation()
                        .distance(e.geometry.getLocalTranslation()) < 0.55f) {
                    rb.add(b); re.add(e);
                }
        for (BulletData b : rb) { bulletsNode.detachChild(b.geometry);  bullets.remove(b); }
        for (EnemyData  e : re) {
            enemiesNode.detachChild(e.geometry);
            enemies.remove(e);
            score += PUNTOS_POR_ENEMIGO; // ← sumar puntos
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
        if (gameOver) return;
        Vector3f pp = player.getLocalTranslation();
        List<BulletData> rb = new ArrayList<>();
        for (BulletData b : enemyBullets)
            if (pp.distance(b.geometry.getLocalTranslation()) < 0.45f) {
                rb.add(b); activarGameOver(); break;
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
        
        // Invencibilidad (solo si tiene más de 1 vida base)
        if (maxVidas() > 1) {
            estaInvencible  = true;
            invencibleTimer = INVENCIBILITY_TIME;
            parpadeoTimer   = PARPADEO_INTERVAL;
            parpadeoVisible = true;
        }
        player.setLocalTranslation(0, 0.3f, 0); // volver al centro
    }

    protected void activarGameOver() {
        if (gameOver) return;
        gameOver = true;
        playerMat.setColor("Color", ColorRGBA.Gray);

        // Mostrar puntuación final en el texto de game over
        hudGameOver.setText("GAME OVER  —  " + score + " pts");
        float W = cam.getWidth();
        hudGameOver.setLocalTranslation(
                (W - hudGameOver.getLineWidth()) / 2f,
                cam.getHeight() / 2f + 60, 0);

        hudGameOver.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        hudReinicio.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
    }

    // ══════════════════════════════════════════════════════
    //  SPAWN DESDE LOS BORDES
    // ══════════════════════════════════════════════════════
    protected void spawnEnemy() {
        Box shape = new Box(0.4f, 0.4f, 0.4f);
        Geometry g = new Geometry("enemy", shape);
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", colorEnemigo());
        g.setMaterial(m);

        float edge = ARENA_SIZE - 0.55f;
        float x, z;
        switch (FastMath.rand.nextInt(4)) {
            case 0 -> { x = rnd(edge); z = -edge; }
            case 1 -> { x = rnd(edge); z =  edge; }
            case 2 -> { x = -edge; z = rnd(edge); }
            default->{ x =  edge; z = rnd(edge); }
        }
        g.setLocalTranslation(x, 0.4f, z);
        enemiesNode.attachChild(g);
        // Offset aleatorio para que los enemigos no disparen todos a la vez
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
        // Usamos nombres únicos con prefijo del modo para no colisionar con el Main
        String pfx = nombreModo().replace(" ", "");
        inputManager.addMapping(pfx+"LEFT",  new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(pfx+"RIGHT", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(pfx+"UP",    new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(pfx+"DOWN",  new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(pfx+"SHOOT", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(inputListener,
                pfx+"LEFT", pfx+"RIGHT", pfx+"UP", pfx+"DOWN", pfx+"SHOOT");
        this.inputPrefix = pfx;
    }

    private String inputPrefix = "";

    private final ActionListener inputListener = (name, isPressed, tpf) -> {
        String pfx = inputPrefix;
        if (name.equals(pfx+"LEFT"))  moveLeft  = isPressed;
        if (name.equals(pfx+"RIGHT")) moveRight = isPressed;
        if (name.equals(pfx+"UP"))    moveUp    = isPressed;
        if (name.equals(pfx+"DOWN"))  moveDown  = isPressed;
        if (name.equals(pfx+"SHOOT") && isPressed) intentarDisparar();
    };

    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════
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
        BulletData(Geometry g, Vector3f d, float l) { geometry=g; direction=d; lifetime=l; }
    }

    protected static class EnemyData {
        Geometry geometry;
        float    shootTimer;
        EnemyData(Geometry g, float st) { geometry = g; shootTimer = st; }
    }
}