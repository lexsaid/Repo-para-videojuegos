package mygame;

import com.jme3.math.ColorRGBA;

/**
 * =========================================================
 * MODO ABANICO
 * =========================================================
 * · Velocidad jugador : lenta (2.5 — mitad de la normal)
 * · Disparo           : 3 balas (0°, +30°, −30°)
 * · Cadencia          : cooldown 0.50 s
 * · Vidas             : 3  (2 s invencibilidad al recibir daño)
 * · Enemigos          : solo persiguen, NO disparan
 * · Spawn inicial     : 6  |  Máximo en pantalla: 30
 * · Suelo             : verde oscuro
 * · Texturas enemigos : Textures/Enemigo_1/  (heredado de ModoBase)
 * =========================================================
 */
public class ModoAbanico extends ModoBase {

    @Override protected float    playerSpeed()      { return 2.5f;  }
    @Override protected float    enemySpeed()       { return 1.8f;  }
    @Override protected float    shootCooldown()    { return 0.50f; }
    @Override protected int      maxVidas()         { return 3;     }
    @Override protected int      maxEnemigos()      { return 30;    }
    @Override protected int      spawnInicial()     { return 6;     }
    @Override protected float    enemySpawnTime()   { return 1.5f;  }
    @Override protected boolean  enemigosDisparan() { return false; }

    @Override
    protected ColorRGBA colorArena() {
        return new ColorRGBA(0.08f, 0.18f, 0.08f, 1f); // verde oscuro
    }

    @Override
    protected ColorRGBA colorEnemigo() {
        return new ColorRGBA(0.9f, 0.15f, 0.15f, 1f); // rojo
    }

    @Override
    protected String nombreModo() { return "Modo Abanico"; }

    // ── NUEVO: Ruta de la música 1 para este Modo ────────────
    @Override
    protected String rutaMusica() {
        return "Sounds/musica1.wav"; // Asegúrate de que el nombre coincida en tu carpeta
    }

    // ModoAbanico también usa Enemigo_1 → NO necesita sobreescribir las rutas.
    // ModoBase ya trae por defecto:
    //   rutaEnemigoDerecha()   = "Textures/Enemigo_1/enemy_"
    //   rutaEnemigoIzquierda() = "Textures/Enemigo_1/EnemyLeft_"
    //   cantidadSkinsEnemigo() = 5

    /** Disparo triple en abanico: bala central, +30° y −30°. */
    @Override
    protected void crearDisparoJugador() {
        crearBalaEnAngulo(  0f);
        crearBalaEnAngulo(+30f);
        crearBalaEnAngulo(-30f);
    }
}