package tracks.singlePlayer.simple.src_erena_casado_david;

import java.util.ArrayList;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import ontology.Types;
import tools.Vector2d;

public class Nodo {
	


	private static ArrayList<Vector2d> posGemas, posPortales;
	private static int avancePorPaso;

	private Nodo parent;
	private ArrayList<ACTIONS> acciones; // La accion con la que se llega a este nodo
	private Vector2d position;
	private Vector2d orientation;
	private int g, h;
	private int f;
	
	private static ArrayList<Vector2d> posicionesObjetosInamovibles;

	public Nodo(StateObservation state, Nodo parent) {
		
		if(posicionesObjetosInamovibles == null) {
			posicionesObjetosInamovibles= new ArrayList<>();
			ArrayList<Observation> aux = state.getImmovablePositions()[0];
			for (int j = 0; j < aux.size(); ++j) {
				posicionesObjetosInamovibles.add(aux.get(j).position);
			}
		}
		
		
		this.parent = parent;
		this.orientation = new Vector2d(state.getAvatarOrientation());
		this.position = new Vector2d(state.getAvatarPosition());

		avancePorPaso = state.getBlockSize();
		posGemas = new ArrayList<>();
		posPortales = new ArrayList<>();


		// Si hay gemas, las añadimos
		if (state.getResourcesPositions() != null) {
			ArrayList<Observation> gemas = state.getResourcesPositions()[0];
			if (!gemas.isEmpty()) {
				for (int i = 0; i < gemas.size(); ++i) {
					posGemas.add(gemas.get(i).position);
				}
			}
		}

		// Si hay portales, los añadimos
		if (state.getPortalsPositions() != null) {
			ArrayList<Observation> portales = state.getPortalsPositions()[0];
			if (!portales.isEmpty()) {
				for (int i = 0; i < portales.size(); ++i) {
					posPortales.add(portales.get(i).position);
				}
			}
		}
		
		
		if (parent != null) {
			this.g = parent.g + 1;
		} else {
			this.g = 0;
		}
		
		this.h = funcionHeuristica();
		this.f=this.g+this.h;
	}

	public Nodo(Nodo parent, ACTIONS accion) {
		acciones = new ArrayList<>();
		this.parent = parent; //Cuidado con punteros

		if (parent.orientation.x == 0.0 && parent.orientation.y == -1.0) { // Mira hacia arriba
			switch (accion) {
			case ACTION_UP: // Ir hacia arriba
				this.g = parent.g + 1;
				acciones.add(Types.ACTIONS.ACTION_UP);
				break;
			default: // Cualquier otro movimiento (No se generan otras acciones que no sean de movimiento)
				this.g = parent.g + 2;
				acciones.add(accion);
				acciones.add(accion);
				break;
			}
		} else if (parent.orientation.x == 0.0 && parent.orientation.y == 1.0) { // Mira hacia abajo
			switch (accion) {
			case ACTION_DOWN: // Ir abajo
				this.g = parent.g + 1;
				acciones.add(Types.ACTIONS.ACTION_DOWN);
				break;
			default:
				this.g = parent.g + 2;
				acciones.add(accion);
				acciones.add(accion);
				break;
			}
		} else if (parent.orientation.x == 1.0 && parent.orientation.y == 0.0) { // Mira hacia derecha
			switch (accion) {
			case ACTION_RIGHT: // Ir hacia la derecha
				this.g = parent.g + 1;
				acciones.add(Types.ACTIONS.ACTION_RIGHT);
				break;
			default:
				this.g = parent.g + 2;
				acciones.add(accion);
				acciones.add(accion);
				break;
			}
		} else { // Otro caso - Mira hacia izquierda
			switch (accion) {
			case ACTION_LEFT: // Ir hacia la izquierda
				this.g = parent.g + 1;
				acciones.add(Types.ACTIONS.ACTION_LEFT);
				break;
			default:
				this.g = parent.g + 2;
				acciones.add(accion);
				acciones.add(accion);
				break;
			}
		}

		switch (accion) {
		case ACTION_UP: // Ir hacia arriba
			this.orientation = new Vector2d(0.0, -1.0);
			this.position = new Vector2d(parent.position.x, parent.position.y - avancePorPaso);
			break;
		case ACTION_RIGHT: // Ir hacia la derecha
			this.orientation = new Vector2d(1.0, 0.0);
			this.position = new Vector2d(parent.position.x + avancePorPaso, parent.position.y);
			break;
		case ACTION_LEFT: // Ir hacia la izquierda
			this.orientation = new Vector2d(-1.0, 0.0);
			this.position = new Vector2d(parent.position.x - avancePorPaso, parent.position.y);
			break;
		case ACTION_DOWN: // Ir abajo
			this.orientation = new Vector2d(0.0, 1.0);
			this.position = new Vector2d(parent.position.x, parent.position.y + avancePorPaso);
			break;
		default: // Resto de acciones - No debería llegar a usarse
			this.orientation = new Vector2d(parent.orientation);
			this.position = new Vector2d(parent.position);
			break;

		}
		
		this.h = funcionHeuristica();
		this.f=this.g+this.h;
	}

	// Constructor de copia
	public Nodo(Nodo nodo) {
		if (nodo != null && nodo != this) {
			if (nodo.parent != null)
				this.parent = nodo.parent; // Esto será recursivo hasta el padre raíz - Horrible en espacio, pero nos ahorramos problemas de cambios.
			this.orientation = new Vector2d(nodo.orientation);
			this.position = new Vector2d(nodo.position);
			this.g = nodo.g;
			this.h = nodo.h;
			this.f = nodo.f;
			if (nodo.acciones != null)
				this.acciones = new ArrayList<>(nodo.acciones);
		}
	}

	@Override
	public boolean equals(Object o) {

		// If the object is compared with itself then return true
		if (o == this) {
			return true;
		}

		/*
		 * Check if o is an instance of Nodo or not "null instanceof [type]" also
		 * returns false
		 */
		if (!(o instanceof Nodo)) {
			return false;
		}

		// typecast o to Nodo so that we can compare data members
		Nodo c = (Nodo) o;

		// Compare the data members and return accordingly
		return position.equals(c.position) == true && orientation.equals(c.orientation) == true;
	}

	public int getF() {
		return f;
	}

	public Nodo getParent() {
		return parent;
	}

	public int getG() {
		return g;
	}

	public int getH() {
		return h;
	}

	public ACTIONS getAccion() { // Como el vector solo contiene una repetida, solo devolvemos la primera
		return acciones.get(0);
	}

	public ArrayList<ACTIONS> getAcciones() {
		return acciones;
	}

	public Vector2d getPosition() {
		return this.position;
	}

	public Vector2d getOrientation() {
		return this.orientation;
	}

	// Esto lo hace todo bien parece
	public ArrayList<Nodo> GenerarHijos(StateObservation state) { // Aquí hay que hacer movimientos solo, no cambios de
																// orientación (Más eficiente de esta forma).
		
		ArrayList<Nodo> hijos = new ArrayList<>();
		boolean contiene;
		
		for (int i = 1; i < Agent.actions.length; ++i) { // i no empieza en 0 para generar nodos con movimiento solo
			// Si la posición a la que se movería (fácil por ahorrar giros) es inamovible,
			// no generamos nodo.
			Vector2d posicionFutura = new Vector2d(this.position);
			contiene=true;
			switch (Agent.actions[i]) {
			case ACTION_UP:
				posicionFutura.y -= avancePorPaso;
				contiene = posicionesObjetosInamovibles.contains(posicionFutura);
				break;
			case ACTION_RIGHT:
				posicionFutura.x += avancePorPaso;
				contiene = posicionesObjetosInamovibles.contains(posicionFutura);
				break;
			case ACTION_LEFT:
				posicionFutura.x -= avancePorPaso;
				contiene = posicionesObjetosInamovibles.contains(posicionFutura);
				break;
			case ACTION_DOWN:
				posicionFutura.y += avancePorPaso;
				contiene = posicionesObjetosInamovibles.contains(posicionFutura);
				break;
			default:
				break;
			}
			if (!contiene) {
				hijos.add(new Nodo(this, Agent.actions[i]));
			}
		}
		return hijos;
	}
	
	private int distanciaManhattan(Vector2d a, Vector2d b) {
		return (int) (Math.abs(a.x - b.x) + Math.abs(a.y - b.y));
	}
	
	public int funcionHeuristica() {

		int resultado = 1000;
		int gemasRecogidas = Agent.numGemas;
		if(!posGemas.isEmpty() && (gemasRecogidas <10) ) {
			if(posGemas.contains(this.position)) {
				return 0;
			}
			else {
//				GREEDY ANTICIPANDO CON UNA GEMA DE ANTICIPACION - Cogemos el minimo de (distancia Jugador Gema + minima distancia Gema a otra Gema)
				//Calculamos el valor con la gema 0 para comparar con el resto
				Vector2d gema = posGemas.get(0);
				int minimo = distanciaManhattan(this.position, gema); //Distancia con el jugador
				if(posGemas.size()>1) { //Si hay más de una gema
					
					Vector2d gemaAux = posGemas.get(1);
					int minAux = distanciaManhattan(gema, gemaAux);//Distancia con la gema 1 (Para comparar)
					int aux = minAux;
					
					for(int i=2; i< posGemas.size(); ++i) { // Minimo de distancia de la gema 0 con el resto (en este caso gemas 2-posgemas.size)
						gemaAux = posGemas.get(i);
						aux = distanciaManhattan(gema, gemaAux);
						if(aux < minAux) {
							minAux = aux;
						}
						
					}
					
					minimo += minAux; //Ya tenemos el primer valor (distancia manhattan jugador-gema0 + menor distancia manhattan gema0-gema_i)
					
					
					for(int i=1; i<posGemas.size(); ++i) {
						gema = posGemas.get(i);
						int distanciaJugador = distanciaManhattan(this.position, gema);
						
						gemaAux = posGemas.get(0); //Empezamos a comparar con el resto de gemas
						minAux = distanciaManhattan(gema, gemaAux);
						aux = minAux;
						for(int j = 0; j<posGemas.size(); ++j) {
							if(j != i) { //Si comparamos con ella misma nos sale 0
								gemaAux = posGemas.get(j);
								aux = distanciaManhattan(gema, gemaAux);
								
								if(aux < minAux) {
									minAux = aux;
								}
							}
						}
							
						
						int nuevaH = distanciaJugador + minAux;
						
						if(nuevaH < minimo) {
							minimo = nuevaH;
						}
						
					}
				}
				resultado = minimo;
			}
			
		}
		else {
			if (posPortales.contains(this.position)) {
				return 0;
			} else {

				Vector2d portal = posPortales.get(0);
				int minimo = distanciaManhattan(this.position, portal);
				int aux;

				for (int i = 1; i < posPortales.size(); ++i) {
					portal = posPortales.get(i);
					aux = distanciaManhattan(this.position, portal);
					if (aux < minimo)
						minimo = aux;
				}

				resultado = minimo;
			}
		}
		
		return resultado/avancePorPaso;
	}
	
	@Override
    public int hashCode() {
        return (int)((this.orientation.y+1)*5 + (this.position.x+1)*11 + 
                (this.position.y+1)*13);
    }

}