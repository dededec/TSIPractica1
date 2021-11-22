package tracks.singlePlayer.simple.src_erena_casado_david;
import java.util.Random;
import java.util.ArrayList;
import tools.Vector2d;
import core.game.Observation;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.HashSet;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;

  

public class Agent extends AbstractPlayer {
	
	public static Random random;
	public static ACTIONS[] actions;

	private HashSet<Nodo> cerrados  = new HashSet<Nodo>();
	private PriorityQueue<Nodo> abiertos;
	

	private ArrayList<ArrayList<Integer>> mapaPrioridades = new ArrayList<>();
	
	public static int numGemas;
	
	private ArrayList<ACTIONS> plan;
	boolean sinEnemigos;
	
	
	
	
	public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
		
		random = new Random();
		actions = so.getAvailableActions().toArray(new ACTIONS[0]);
		
		numGemas = 0;

		abiertos = new PriorityQueue<Nodo>(new Comparator<Nodo>() {
			@Override
			public int compare(Nodo primero, Nodo segundo) {
				return primero.getF() - segundo.getF();
			}
		});
		
		if(so.getNPCPositions() != null) sinEnemigos = so.getNPCPositions()[0].isEmpty();
		else sinEnemigos=true;
		

		plan = new ArrayList<>();
		
		/*--------------------- MATRIZ MAPA PRIORIDADES ---------------------*/
		//Inicializamos esquinas a valor pequeño, y alrededor a algo menos
		int valorEsquina = 2;
		int valorAlrededorEsquina = 3;
		int valorMenor = 4;
		int valorPared = 0;
		
		//Inicializamos a valorMenor
		for(int i=0; i<13; ++i) {
			ArrayList<Integer> aux = new ArrayList<>();
			for(int j=0; j<26; ++j) {
				aux.add(valorMenor);
			}
			mapaPrioridades.add(aux);
		}
		
		//Primera fila
		mapaPrioridades.get(1).set(1, valorEsquina);
		mapaPrioridades.get(1).set(24, valorEsquina);
		
		mapaPrioridades.get(1).set(2, valorEsquina);
		mapaPrioridades.get(1).set(23, valorEsquina);
		
		mapaPrioridades.get(1).set(3, valorAlrededorEsquina);
		mapaPrioridades.get(1).set(22, valorAlrededorEsquina);
		
		//Ultima fila
		mapaPrioridades.get(11).set(1, valorEsquina);
		mapaPrioridades.get(11).set(24, valorEsquina);
		
		mapaPrioridades.get(11).set(2, valorEsquina);
		mapaPrioridades.get(11).set(23, valorEsquina);
		
		mapaPrioridades.get(11).set(3, valorAlrededorEsquina);
		mapaPrioridades.get(11).set(22, valorAlrededorEsquina);
		
		for(int i=4; i<13; ++i) { //Recorremos hasta la mitad
			mapaPrioridades.get(1).set(i, valorAlrededorEsquina);
			mapaPrioridades.get(1).set(25-i, valorAlrededorEsquina);
			mapaPrioridades.get(11).set(i, valorAlrededorEsquina);
			mapaPrioridades.get(11).set(25-i, valorAlrededorEsquina);
		}
		

		
		//Rellenamos valores al lado de las paredes
		mapaPrioridades.get(2).set(1, valorEsquina);
		mapaPrioridades.get(2).set(2, valorAlrededorEsquina);
		mapaPrioridades.get(2).set(23, valorAlrededorEsquina);
		mapaPrioridades.get(2).set(24, valorEsquina);
		
		mapaPrioridades.get(10).set(1, valorEsquina);
		mapaPrioridades.get(10).set(2, valorAlrededorEsquina);
		mapaPrioridades.get(10).set(23, valorAlrededorEsquina);
		mapaPrioridades.get(10).set(24, valorEsquina);
		
		for(int i=3; i<7; ++i) { //Recorremos hasta la mitad
			mapaPrioridades.get(i).set(1, valorAlrededorEsquina);
			mapaPrioridades.get(i).set(24, valorAlrededorEsquina);
			mapaPrioridades.get(12-i).set(1, valorAlrededorEsquina);
			mapaPrioridades.get(12-i).set(24, valorAlrededorEsquina);
		}
		
		//Ponemos valorPared en las paredes que haya esparcidas por el mapa
		int avance = so.getBlockSize();
		ArrayList<Observation> aux = so.getImmovablePositions()[0];
		ArrayList<Vector2d> posMatrizMuro = new ArrayList<>();
		for (int j = 0; j < aux.size(); ++j) {
			Vector2d posMatriz = new Vector2d(aux.get(j).position.y/avance, aux.get(j).position.x/avance);
			posMatrizMuro.add(posMatriz);
		}
		
		//Tenemos que ver si hay huecos estrechos, y ponerle valorEsquina o valorPared
		//Los muros tienen que estar en la misma fila o en la misma columna
		//La diferencia en el otro tiene que ser 3 o menor
		/*Hueco estrecho:
		 * w--w
		 * 
		 * w
		 * -
		 * -
		 * w
		 * */
		for(int i=0; i<posMatrizMuro.size(); ++i) {
			Vector2d pos = posMatrizMuro.get(i);
			if(pos.x >1 && pos.x < mapaPrioridades.size()-1 && pos.y >1 && pos.y < mapaPrioridades.get(0).size()-1) {
				for(int j=i+1; j<posMatrizMuro.size(); ++j) {
					Vector2d pos2 = posMatrizMuro.get(j);
					double difFil = Math.abs(pos.x - pos2.x);
					double difCol = Math.abs(pos.y - pos2.y);
					if(difFil == 0 && difCol <= 3 && difCol > 0) { //Comparten fila
						int low = (int)pos.y;
						int high = (int)pos2.y;
						if(pos2.y < pos.y) {
							low = (int)pos2.y;
							high = (int)pos.y;
						}
						for(int k=low; k<high; ++k) {
							mapaPrioridades.get((int)pos.x).set(k, valorAlrededorEsquina);
						}
					}
					else if(difCol == 0 && difFil <=3 && difFil > 0) {
						int low = (int)pos.x;
						int high = (int)pos2.x;
						if(pos2.x < pos.x) {
							low = (int)pos2.x;
							high = (int)pos.x;
						}
						for(int k=low; k<high; ++k) {
							mapaPrioridades.get(k).set((int)pos.y, valorAlrededorEsquina);
						}
					}
				}
			}
			
		}
		
		for(int i=0; i<posMatrizMuro.size();++i) {
			Vector2d muro = posMatrizMuro.get(i);
			mapaPrioridades.get((int)muro.x).set((int)muro.y, valorPared);
		}
		
		//Valor paredes exteriores
		for(int i=0; i<26; ++i) {
			mapaPrioridades.get(0).set(i, valorPared);
			mapaPrioridades.get(12).set(i, valorPared);
		}
		
		for(int i=0; i<13;++i) {
			mapaPrioridades.get(i).set(0, valorPared);
			mapaPrioridades.get(i).set(25, valorPared);
		}
		/*--------------------- FIN MAPA PRIORIDADES ---------------------*/
		
	}
	
	private Nodo SelectNode() {
		return abiertos.poll();
	}

	private ACTIONS comportamientoDeliberativo(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		
		double worstCase = 10;
		double avgTime = 10;
		double totalTime = 0;
		double iteration = 0;
		
		if (!plan.isEmpty()) { // Tenemos un plan hecho
			return plan.remove(plan.size() - 1);
		}
		
		
		Nodo root = new Nodo(stateObs, null);
		abiertos.add(root);
		

		Nodo nodoActual;
		

		// A*
		while (elapsedTimer.remainingTimeMillis() > 2 * avgTime && elapsedTimer.remainingTimeMillis() > worstCase) {
			ElapsedCpuTimer temp = new ElapsedCpuTimer();

			
			if (abiertos.isEmpty()) { // abiertos vacío
				System.out.println("Break ------------------- ABIERTOS vacío.");
				break;
			}

			nodoActual = SelectNode(); // No da null porque miramos el tamaño de abiertos antes

			if (nodoActual.getH() == 0) { // Si es nodo solución salimos

				while (nodoActual.getParent() != null) // Rellenamos plan
				{
					plan.addAll(nodoActual.getAcciones());
					nodoActual = nodoActual.getParent();
				}
				abiertos.clear(); //Limpiamos para planificar para otra gema
				cerrados.clear();
				return plan.remove(plan.size() - 1);
			}
			
			ArrayList<Nodo> hijos = nodoActual.GenerarHijos(stateObs);
			
			for(int i=0; i<hijos.size();++i) {
				Nodo hijo = hijos.get(i);
				if(cerrados.contains(hijo) == false) {
					abiertos.add(hijo);
				}
			}
			cerrados.add(nodoActual);

			//A* normal (por tenerlo de referencia)
//			Nodo puntero;
//			Iterator<Nodo> it;
//
//			for (int i = 0; i < hijos.size(); ++i) { // Insertamos hijos
//				Nodo hijo = hijos.get(i);
//
//				if (abiertos.contains(hijo)) { // Ya estaba en abiertos
//					it = abiertos.iterator();
//					while (it.hasNext()) {
//						puntero = it.next();
//
//						if (puntero.equals(hijo)) {
//							if (puntero.getF() > hijo.getF()) {
//								puntero = new Nodo(hijo);
//							}
//							break;
//						}
//					}
//
//				} 
//				else if (cerrados.contains(hijo) == true) { // Está en cerrados
//					it = cerrados.iterator();
//					while(it.hasNext()) {
//						Nodo elemento = it.next();
//						if(elemento.equals(hijo)) {
//							if(elemento.getF() > hijo.getF()) {
//								cerrados.remove(elemento);
//								abiertos.add(hijo);
//								
//								Iterator<Nodo> it2 = abiertos.iterator();
//								while(it2.hasNext()) {
//									Nodo elementoAbiertos = it2.next();
//									if(elementoAbiertos.getParent() != null && elementoAbiertos.getParent().equals(hijo)) {
//										elementoAbiertos = new Nodo(hijo, elementoAbiertos.getAccion());
//									}
//								}
//							}
//							
//							break;
//						}
//					}
//				}
//				else {
//					abiertos.add(hijo);
//				}
//			}
//			cerrados.add(nodoActual);

			// Actualizamos tiempo
			totalTime += temp.elapsedMillis();
			iteration += 1;
			avgTime = totalTime / iteration;
		}
		
		System.out.println("Out of time");
		return Types.ACTIONS.ACTION_NIL;
	}
	
	private int distanciaManhattan(Vector2d a, Vector2d b) {
		return (int) (Math.abs(a.x - b.x) + Math.abs(a.y - b.y));
	}
	
	private int valorMapaPrioridades(Vector2d pos, int blockSize) {
		Vector2d posicionMatriz = new Vector2d(pos.y / blockSize, pos.x/blockSize);
		return mapaPrioridades.get((int)posicionMatriz.x).get((int)posicionMatriz.y);
	}
	
	private ACTIONS comportamientoReactivo(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		
		ArrayList<Vector2d> posEnemigos = new ArrayList<>();
		if(stateObs.getNPCPositions().length > 0) {
			for(int i=0; i<stateObs.getNPCPositions()[0].size(); ++i) {
				posEnemigos.add(stateObs.getNPCPositions()[0].get(i).position);
			}
		}
		
		int avance = stateObs.getBlockSize();
		double aux = 0;
		double maximo = 0;
		int mejorAccion = -1;
		for(int i=1; i<actions.length;++i) {
			Vector2d posFutura = new Vector2d(stateObs.getAvatarPosition());
			ACTIONS accion = actions[i];
			switch(accion) {
			case ACTION_UP:
				posFutura.y -= avance;
				break;
			case ACTION_RIGHT:
				posFutura.x += avance;
				break;
			case ACTION_LEFT:
				posFutura.x -= avance;
				break;
			case ACTION_DOWN:
				posFutura.y += avance;
				break;
			default:
				break;
			}
			
			//Cogemos distancia menor al jugador
			aux= distanciaManhattan(posFutura, posEnemigos.get(0));
			for(int j=1; j<posEnemigos.size(); ++j) {
				int d = distanciaManhattan(posFutura, posEnemigos.get(j));
				if(aux > d) {
					aux = distanciaManhattan(posFutura, posEnemigos.get(j));
				}
			}
			
			double mul = (double)valorMapaPrioridades(posFutura, avance)/2;
			aux *= mul; //No priorizar las esquinas
			if(aux > maximo) {
				maximo = aux;
				mejorAccion = i;
			}		
		}
		
		if(mejorAccion == -1) return Types.ACTIONS.ACTION_NIL;
		return actions[mejorAccion];
	}
	
	private ACTIONS comportamientoDeliberativoReactivo(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		int avance = stateObs.getBlockSize();
		boolean usarReactivo=false;
		
		ArrayList<Vector2d> posEnemigos = new ArrayList<>();
		if(stateObs.getNPCPositions().length > 0) {
			for(int i=0; i<stateObs.getNPCPositions()[0].size(); ++i) {
				posEnemigos.add(stateObs.getNPCPositions()[0].get(i).position);
			}
		}
		
		for(int i=0; i<posEnemigos.size(); ++i) {
			if(distanciaManhattan(stateObs.getAvatarPosition(), posEnemigos.get(i)) <= 4*avance) {
				usarReactivo = true;
				break;
			}
		}
		
		if(usarReactivo) {
			abiertos.clear();
			cerrados.clear();
			return comportamientoReactivo(stateObs, elapsedTimer);
		}
		else {
			return comportamientoDeliberativo(stateObs, elapsedTimer);
		}
	}
	
	
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		
		Iterator<Integer> it = stateObs.getAvatarResources().values().iterator();
		if(it.hasNext()) {
			numGemas = it.next();
		}
		
		boolean noHayGemas = stateObs.getResourcesPositions() == null && numGemas < 10;
		
		if(sinEnemigos) { 
			return comportamientoDeliberativo(stateObs, elapsedTimer);
		}
		else{
			if(noHayGemas) {
				return comportamientoReactivo(stateObs, elapsedTimer);
			}
			else{
				return comportamientoDeliberativoReactivo(stateObs, elapsedTimer);
			}
		}
		
	}
}
