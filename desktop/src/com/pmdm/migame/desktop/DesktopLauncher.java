package com.pmdm.migame.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.pmdm.migame.MiJuego;
/*
public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(new MiJuego(), config);
	}
}
*/
public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		//Indicamos el título de la ventana del juego.
		config.title = "Hora de aventuras";
		//Indicamos la anchura de la ventana del juego.
		config.width = 800;
		//Indicamos la altura de la ventana del juego.
		config.height = 480;
		//Lanzamos el juego en su versión de escritorio.
		new LwjglApplication(new MiJuego(), config);
	}
}
