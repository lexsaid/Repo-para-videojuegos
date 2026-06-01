package mygame;
 
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
 
/**
 * =========================================================
 * MODO ZOMBIES
 * =========================================================
 * · Velocidad jugador : rápida (6.5)
 * · Personaje         : Personaje_2
 * · Arma              : Ametralladora — 1 bala con dispersión ±15°
 * · Cadencia          : 0.12 s
 * · Balas totales     : 100
 * · Vidas             : 3
 * · Enemigos          : Enemigo_3, solo hacen daño al tocar (sin disparo)
 * · Spawn inicial     : 8  |  Máximo: 40  |  Spawn: 0.4 s
 * · Escenario         : Escenario3
 * · HUD               : esquina superior derecha
 * =========================================================
 */
public class ModoZombies extends ModoBase {
 
    // ── Munición ─────────────────────────────────────────────
    private int balasRestantes = 100;
 
    // ── HUD propio (esquina superior derecha) ────────────────
    private BitmapText hudVidasZ;
    private BitmapText hudBalasZ;
    private BitmapText hudScoreZ;
    private BitmapText hudCooldownZ;
    private BitmapText hudControlesZ;
 
    // ── Parámetros ───────────────────────────────────────────
    @Override protected float    playerSpeed()      { return 6.5f;  }
    @Override protected float    enemySpeed()       { return 1.2f;  }
    @Override protected float    shootCooldown()    { return 0.12f; }
    @Override protected int      maxVidas()         { return 3;     }
    @Override protected int      maxEnemigos()      { return 40;    }
    @Override protected int      spawnInicial()     { return 8;     }
    @Override protected float    enemySpawnTime()   { return 0.4f;  }
 
    // ── Zombies solo dañan al tocar, no disparan ─────────────
    @Override protected boolean  enemigosDisparan() { return false; }
    @Override protected boolean  disparoAutomatico() { return true;  }
 
    @Override protected ColorRGBA colorArena()   { return new ColorRGBA(0.05f, 0.12f, 0.02f, 1f); }
    @Override protected ColorRGBA colorEnemigo() { return new ColorRGBA(0.2f, 0.55f, 0.1f, 1f);  }
    @Override protected String    nombreModo()   { return "ModoZombies"; }
 
    // ── Música ───────────────────────────────────────────────
    @Override
    protected String rutaMusica() { return "Sounds/musica4.wav"; }
 
    // ── Escenario y Texturas ─────────────────────────────────
    @Override protected String rutaEscenario()          { return "Textures/Fondo/Escenario3.png";      }
    @Override protected String rutaPersonajeDerecha()   { return "Textures/Personaje_2/Pj2Right_";      }
    @Override protected String rutaPersonajeIzquierda() { return "Textures/Personaje_2/Pj2Left_";       }
    @Override protected int    frameInicialPersonaje()  { return 1; } // archivos van de _1 a _4
    @Override protected int    cantidadFramesPersonaje(){ return 4; } // 4 frames: 1,2,3,4
    @Override protected String rutaEnemigoDerecha()     { return "Textures/Enemigo_3/PjRight_";        }
    @Override protected String rutaEnemigoIzquierda()   { return "Textures/Enemigo_3/PjLeft_";         }
    @Override protected int    cantidadSkinsEnemigo()   { return 4; }
 
    // ── Munición ─────────────────────────────────────────────
    @Override
    public int getBalasRestantes() { return balasRestantes; }
 
    public void recargarMunicion() {
        balasRestantes = 100;
        // Solo recarga balas, los enemigos en pantalla NO se tocan
    }
 
    // ── Reinicio ─────────────────────────────────────────────
    @Override
    public void onReiniciar() {
        balasRestantes = 100;
        super.onReiniciar();
        actualizarHUDZombies();
    }
 
    // ── HUD propio ───────────────────────────────────────────
    @Override
    public void iniciar(Node rootNode, Node guiNode,
                        AssetManager assetManager,
                        InputManager inputManager,
                        Camera cam,
                        BitmapFont guiFont) {
        super.iniciar(rootNode, guiNode, assetManager, inputManager, cam, guiFont);
        setupHUDZombies();
    }
 
    private void setupHUDZombies() {
        float fs = guiFont.getCharSet().getRenderedSize();
        ocultarHUDBase();
 
        hudVidasZ = crearTextoHUD("", fs * 1.4f, ColorRGBA.Red);
        modoGui.attachChild(hudVidasZ);
 
        hudBalasZ = crearTextoHUD("", fs * 1.4f, ColorRGBA.Cyan);
        modoGui.attachChild(hudBalasZ);
 
        hudScoreZ = crearTextoHUD("", fs * 1.4f, ColorRGBA.Yellow);
        modoGui.attachChild(hudScoreZ);
 
        hudCooldownZ = crearTextoHUD("", fs, ColorRGBA.Green);
        modoGui.attachChild(hudCooldownZ);
 
        hudControlesZ = crearTextoHUD(
            "WASD: Mover  |  ESPACIO: Disparar  |  R: Recargar  |  ESC: Menú",
            fs * 0.9f, new ColorRGBA(0.8f, 0.8f, 0.8f, 1f));
        hudControlesZ.setLocalTranslation(10, cam.getHeight() - 10, 0);
        modoGui.attachChild(hudControlesZ);
 
        actualizarHUDZombies();
    }
 
    @Override
    protected void actualizarHUDExtra(float tpf) {
        actualizarHUDZombies();
    }
 
    private void actualizarHUDZombies() {
        if (hudVidasZ == null) return;
 
        float W      = cam.getWidth();
        float H      = cam.getHeight();
        float fs     = guiFont.getCharSet().getRenderedSize();
        float margen = 10f;
        float lineaY = H - 10f;
 
        // Vidas
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vidas;        i++) sb.append("♥ ");
        for (int i = vidas; i < maxVidas(); i++) sb.append("♡ ");
        hudVidasZ.setText("Vidas: " + vidas + "  " + sb);
        hudVidasZ.setLocalTranslation(W - hudVidasZ.getLineWidth() - margen, lineaY, 0);
        lineaY -= fs * 1.8f;
 
        // Balas
        hudBalasZ.setText("Balas: " + balasRestantes);
        hudBalasZ.setColor(balasRestantes > 20 ? ColorRGBA.Cyan : ColorRGBA.Red);
        hudBalasZ.setLocalTranslation(W - hudBalasZ.getLineWidth() - margen, lineaY, 0);
        lineaY -= fs * 1.8f;
 
        // Score
        hudScoreZ.setText("Puntos: " + scorePartida + "  |  Récord: " + scoreMaximo);
        hudScoreZ.setLocalTranslation(W - hudScoreZ.getLineWidth() - margen, lineaY, 0);
        lineaY -= fs * 1.6f;
 
        // Cooldown
        if (shootTimer >= shootCooldown()) {
            hudCooldownZ.setColor(ColorRGBA.Green);
            hudCooldownZ.setText("Disparo: [LISTO]");
        } else {
            hudCooldownZ.setColor(ColorRGBA.Red);
            hudCooldownZ.setText(String.format("Disparo: %.1f s", shootCooldown() - shootTimer));
        }
        hudCooldownZ.setLocalTranslation(W - hudCooldownZ.getLineWidth() - margen, lineaY, 0);
    }
 
    private BitmapText crearTextoHUD(String texto, float size, ColorRGBA color) {
        BitmapText bt = new BitmapText(guiFont, false);
        bt.setSize(size);
        bt.setColor(color);
        bt.setText(texto);
        return bt;
    }
 
    @Override
    protected void crearDisparoJugador() {
        if (balasRestantes <= 0) return;
        float dispersion = (float)(Math.random() * 30f) - 15f;
        crearBalaEnAngulo(dispersion);
        balasRestantes--;
    }
}