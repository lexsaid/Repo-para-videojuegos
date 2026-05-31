package mygame;

import com.jme3.math.ColorRGBA;

/**
 * =========================================================
 * MODO SUPERVIVENCIA
 * =========================================================
 * · Velocidad jugador : normal (5.0)
 * · Disparo           : 3 balas (0°, +30°, −30°)
 * · Cadencia          : cooldown 0.40 s
 * · Vidas             : 1  (cualquier impacto = Game Over)
 * · Enemigos          : persiguen Y disparan cada 2.5 s
 * Sus balas (magenta) van al jugador
 * · Spawn inicial     : 6  |  Máximo en pantalla: 20
 * · Suelo             : rojo oscuro (señal de peligro)
 * · Texturas enemigos : Textures/Enemigo_2/  ← diferente a los otros modos
 * =========================================================
 */
public class ModoSupervivencia extends ModoBase {

    @Override protected float    playerSpeed()      { return 5.0f;  }
    @Override protected float    enemySpeed()       { return 2.0f;  }
    @Override protected float    shootCooldown()    { return 0.40f; }
    @Override protected int      maxVidas()         { return 1;     }
    @Override protected int      maxEnemigos()      { return 20;    }
    @Override protected int      spawnInicial()     { return 6;     }
    @Override protected float    enemySpawnTime()   { return 0.5f;  }
    @Override protected boolean  enemigosDisparan() { return true;  }
    @Override protected float    enemyShootTime()   { return 2.5f;  }
    @Override protected boolean  disparoAutomatico() { return true;  }

    @Override
    protected ColorRGBA colorArena() {
        return new ColorRGBA(0.2f, 0.04f, 0.04f, 1f); // rojo oscuro
    }

    @Override
    protected ColorRGBA colorEnemigo() {
        return new ColorRGBA(1f, 0.38f, 0f, 1f); // naranja
    }

    @Override
    protected String nombreModo() { return "Modo Supervivencia"; }

    // ── NUEVO: Ruta de la música 3 para este Modo ────────────
    @Override
    protected String rutaMusica() {
        return "Sounds/musica3.wav"; // Si da problemas de formato, cámbialo a musica3.wav o musica3.ogg
    }

    // ── Rutas de texturas: Enemigo_2 ────────────────────────
    // Estos tres métodos hacen que ModoBase cargue las imágenes
    // de Textures/Enemigo_2/ en lugar de Textures/Enemigo_1/

    @Override
    protected String rutaEnemigoDerecha() {
        return "Textures/Enemigo_2/Enemy2Right_";
    }

    @Override
    protected String rutaEnemigoIzquierda() {
        return "Textures/Enemigo_2/Enemy2Left_";
    }

    @Override
    protected int cantidadSkinsEnemigo() {
        return 4; // Enemigo_2 tiene 4 frames (0 al 3)
    }

    /** Disparo triple en abanico. */
    @Override
    protected void crearDisparoJugador() {
        crearBalaEnAngulo(  0f);
        crearBalaEnAngulo(+30f);
        crearBalaEnAngulo(-30f);
    }
}