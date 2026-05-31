package mygame;

import com.jme3.math.ColorRGBA;

/**
 * =========================================================
 * MODO CLÁSICO
 * =========================================================
 * · Velocidad jugador : normal (5.0)
 * · Disparo           : 1 bala al frente
 * · Cadencia          : rápida (cooldown 0.20 s)
 * · Vidas             : 3  (2 s invencibilidad al recibir daño)
 * · Enemigos          : solo persiguen, NO disparan
 * · Spawn inicial     : 6  |  Máximo en pantalla: 20
 * · Suelo             : gris azulado
 * · Texturas enemigos : Textures/Enemigo_1/  (heredado de ModoBase)
 * =========================================================
 */
public class ModoClasico extends ModoBase {

    @Override protected float    playerSpeed()      { return 5.0f;  }
    @Override protected float    enemySpeed()       { return 1.8f;  }
    @Override protected float    shootCooldown()    { return 0.20f; }
    @Override protected int      maxVidas()         { return 3;     }
    @Override protected int      maxEnemigos()      { return 20;    }
    @Override protected int      spawnInicial()     { return 6;     }
    @Override protected float    enemySpawnTime()   { return 1.5f;  }
    @Override protected boolean  enemigosDisparan() { return false; }

    @Override
    protected ColorRGBA colorArena() {
        return new ColorRGBA(0.15f, 0.15f, 0.2f, 1f); // gris azulado
    }

    @Override
    protected ColorRGBA colorEnemigo() {
        return new ColorRGBA(0.9f, 0.1f, 0.1f, 1f); // rojo
    }

    @Override
    protected String nombreModo() { return "Modo Clasico"; }

    // ── NUEVO: Ruta de la música 2 para este Modo ────────────
    @Override
    protected String rutaMusica() {
        return "Sounds/musica2.wav"; // Si te da error de formato, cámbialo a musica2.wav
    }

    // ModoClasico usa Enemigo_1 → NO necesita sobreescribir las rutas.
    // ModoBase ya trae por defecto:
    //   rutaEnemigoDerecha()   = "Textures/Enemigo_1/enemy_"
    //   rutaEnemigoIzquierda() = "Textures/Enemigo_1/EnemyLeft_"
    //   cantidadSkinsEnemigo() = 5

    /** Disparo simple: 1 bala al frente. */
    @Override
    protected void crearDisparoJugador() {
        crearBalaEnAngulo(0f);
    }
}