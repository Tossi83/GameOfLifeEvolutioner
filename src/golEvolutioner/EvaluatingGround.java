package golEvolutioner;

import java.util.Arrays;

//import javafx.animation.KeyFrame;
//import javafx.animation.Timeline;
//import javafx.event.ActionEvent;
//import javafx.event.EventHandler;
//import javafx.util.Duration;

public class EvaluatingGround
	{
	private boolean livingAreasEquals(HistoryPage la1, HistoryPage la2)
		{
		/*for (int ix = 0; ix < 256; ix++)
			for (int iy = 0; iy < 256; iy++)
				if (la1.livingArea[ix][iy] != la2.livingArea[ix][iy]) return false;*/
		for (int ix = 0; ix < 256; ix++)
			for (int iy = 0; iy < 4; iy++)
				if (la1.rowSum[ix][iy] != la2.rowSum[ix][iy]) return false;
		return true;
		}
	
	private class Coordinates
		{
		public int x, y;
		public Coordinates(int x, int y)
			{
			this.x = x;
			this.y = y;
			}
		}
	
	private void addCellToCheck(int x, int y)
		{
		x = x & 255;
		y = y & 255;
		if (!addedToCheck[x][y])
			{
			cellsToCheck[cellsToCheckSize++] = new Coordinates(x, y);
			addedToCheck[x][y] = true;
			}
		}
	
	private void updateNeighboursCounts(int x, int y, boolean born)
		{
		int xmax = x + 2,
			ymax = y + 2;
		for (int ix = x - 1; ix < xmax; ix++)
			for (int iy = y - 1; iy < ymax; iy++)
				{
				if ((ix != x) || (iy != y)) addCellToCheck(ix, iy);
				if (born) neighboursCounts[ix & 255][iy & 255]++;
					else neighboursCounts[ix & 255][iy & 255]--;
				}
		}

	private void alterCell(int x, int y, int historyIndex)
		{
		//history[historyIndex].livingArea[x][y] = !history[historyIndex].livingArea[x][y];
		int iY = y >> 6;
		long  vY = 1L << (y & 63);
		history[historyIndex].rowSum[x][iY] = history[historyIndex].rowSum[x][iY] ^ vY;
		/*if (history[historyIndex].livingArea[x][y]) history[historyIndex].cellCount++;
			else history[historyIndex].cellCount--;
		updateNeighboursCounts(x, y, history[historyIndex].livingArea[x][y]);*/
		if ((history[historyIndex].rowSum[x][iY] & vY) != 0)
			{
			history[historyIndex].cellCount++;
			updateNeighboursCounts(x, y, true);
			}
			else
			{
			history[historyIndex].cellCount--;
			updateNeighboursCounts(x, y, false);
			}
		}
	
	private void alterCellField(int x, int y)
		{
		//history[0].livingArea[x][y] = !history[0].livingArea[x][y];
		int iY = y >> 6;
		long  vY = 1L << (y & 63);
		history[0].rowSum[x][iY] = history[0].rowSum[x][iY] | vY;
		//addFieldToCheck(x, y);
		addCellToCheck(x, y);
		updateNeighboursCounts(x, y, true);
		}
	
	class ThreadEvaluation extends Thread
		{		
		@Override
		public void run()
			{
			// copy living area to history . living area is in history from the start
			// System.out.println("Individual evaluation thread started ... ");
			individual.evaluation = new EvaluationData();
			individual.evaluation.size = individual.width * individual.height;
			individual.evaluation.evaluationTime = System.currentTimeMillis();
			int
				offsetX = (256 - individual.width)/2,
				offsetY = (256 - individual.height)/2;
			for (int x = 0; x < individual.width; x++)
				for (int y = 0; y < individual.height; y++)
					{
					if (individual.body[x][y])
						{
						individual.evaluation.cellCount++;
						alterCellField(x + offsetX, y + offsetY);
						}
					}
			history[0].cellCount = individual.evaluation.cellCount;
			
			// do life step loop
			long timeStamp = System.currentTimeMillis() + 1000;
			int cycleCounter = 0;
			
			while (doLifeStep() && !GolEvolutioner.status.isShuttingDown)
				{
				cycleCounter++;
				if (System.currentTimeMillis() > timeStamp)
					{
					timeStamp = timeStamp + 1000;
					speedThread = cycleCounter;
					cycleCounter = 0;
					}
				}
			// get living area cell count . done in life step
			// if 0 go out (v) . done in life step
			// compare living area to history . done in lifestep
			// if not found . done in life step
			//   increase cyclesLived by 1 . done in life step
			//   copy living area to history and segregate it . done in life step
			//   go back (^) . done in life step
			// if found . done in life step
			//   calculate loopLength and go out (v) . done in life step
			if (individual.evaluation.cellCount == 0)
				{
				individual.evaluation.fitness = 0;
				// System.out.println(" * fitness = 0 because of division ");
				}
				else
				{
				individual.evaluation.fitness
					= 100 *
					(((double)(individual.evaluation.cyclesLived + individual.evaluation.loopLength))
							/((double)individual.evaluation.cellCount));
				// System.out.println(" * fitness calculation ");
				}
			// calculate fitness
			individual.evaluation.evaluationTime = System.currentTimeMillis() - individual.evaluation.evaluationTime;
			isRunning = false;
			// System.out.println("Individual evaluation thread ended ... ");
			}
		}

	private Coordinates[] cellsToCheck = new Coordinates[65536];
	private int cellsToCheckSize = 0;
	private boolean[][] addedToCheck = new boolean[256][256];
	private byte[][] neighboursCounts = new byte[256][256];
	private int historySize = 1;
	
	private ThreadEvaluation evaluation;
	public boolean isRunning = false;
	private Individual individual;
	HistoryPage[] history = {new HistoryPage()};
	int[] sortIndex = {0};
	int speedThread = 0;
	
	/*public Timeline startingTimeline = new Timeline
		(
		new KeyFrame
			(
			Duration.millis(1),
			new EventHandler<ActionEvent>()
				{
				public void handle(ActionEvent event)
					{
					evaluation = new ThreadEvaluation();
					evaluation.run();
					}
				}
			)
		);*/
	
	public void startEvaluation()
		{
		evaluation = new ThreadEvaluation();
		evaluation.start();
		}
	/*
	private boolean getCell(int x, int y, int i)
		{
		x = x & 255;
		y = y & 255;
		return history[i].livingArea[x][y];
		}
	
	private int countNeighbours(int x, int y, int i)
		{
		int countNeighbours = 0,
			xmax = x + 2,
			ymax = y + 2;
		for (int ix = x - 1; ix < xmax; ix++)
			for (int iy = y - 1; iy < ymax; iy++)
				if (((ix != x) || (iy != y)) && getCell(ix, iy, i))
					{
					countNeighbours++;
					if (countNeighbours > 3) return countNeighbours;
					}
		return countNeighbours;
		}*/
	
	public void doLifeStep_public()
		{
		System.out.println("Life step started ...");
		doLifeStep();
		System.out.println("Life step done ...");
		}
	
	private boolean doLifeStep()
		{

		//int iNew = history.length, iLast = iNew - 1;
		int iNew = historySize, iLast = iNew - 1;
		if (history[iLast].cellCount == 0) return false;
		//history = Arrays.copyOf(history, iNew + 1);
		if (++historySize > history.length)
			{
			history = Arrays.copyOf(history, history.length*2);
			sortIndex = Arrays.copyOf(sortIndex, history.length);
			}
		
		history[iNew] = new HistoryPage();
		history[iNew].cellCount = history[iLast].cellCount;
		for (int i = 0; i < 256; i++)
			{
			//history[iNew].livingArea[i] = Arrays.copyOf(history[iLast].livingArea[i], 256);
			history[iNew].rowSum[i] = Arrays.copyOf(history[iLast].rowSum[i], 4);
			}
		//step of life in evaluation
			{
			Coordinates[] cellsToUpdate = new Coordinates[cellsToCheckSize];
			int cellsToUpdateSize = 0, x, y, neighboursNumber;

			for (int i = 0; i < cellsToCheckSize; i++)
				{
				x = cellsToCheck[i].x;
				y = cellsToCheck[i].y;
				neighboursNumber = neighboursCounts[x][y];
				/*if		(
					(history[iNew].livingArea[x][y] &&
					(!(neighboursNumber == 2 || neighboursNumber == 3)))
					||
					(!history[iNew].livingArea[x][y] &&
					(neighboursNumber == 3))
					)*/
				boolean isAlive = (history[iNew].rowSum[x][y >> 6] & (1L << (y & 63))) != 0;
				//(history[historyIndex].rowSum[x][iY] & vY) != 0
				//int iY = y >> 6;
				//long  vY = 1L << (y & 63);
				/*if (neighboursNumber == 3)
					{
					if (!history[iNew].livingArea[x][y])
						cellsToUpdate[cellsToUpdateSize++] = new Coordinates(x, y);
					}
					else if (neighboursNumber != 4 && history[iNew].livingArea[x][y])
						cellsToUpdate[cellsToUpdateSize++] = new Coordinates(x, y);*/
				if (neighboursNumber == 3)
					{
					if (!isAlive)
						cellsToUpdate[cellsToUpdateSize++] = new Coordinates(x, y);
					}
					else if (neighboursNumber != 4 && isAlive)
						cellsToUpdate[cellsToUpdateSize++] = new Coordinates(x, y);
				}

			cellsToCheckSize = 0;
			addedToCheck = new boolean [256][256];
			
			for (int i = 0; i < cellsToUpdateSize; i++)
				alterCell(cellsToUpdate[i].x, cellsToUpdate[i].y, iNew);
				
			}
		/*
		for (int x = 0; x < 256; x++)
			for (int y = 0; y < 256; y++)
				{
				if (history[iLast].livingArea[x][y])
					{
					int countNeighbours = countNeighbours(x, y, iLast);
					history[iNew].livingArea[x][y] = ((1 < countNeighbours) && (countNeighbours < 4));
					}
					else
					{
					history[iNew].livingArea[x][y] = (3 == countNeighbours(x, y, iLast));
					}
				if (history[iNew].livingArea[x][y]) history[iNew].cellCount++;
				}*/
		
		if (history[iNew].cellCount == 0)
			{
			//sortIndex = Arrays.copyOfRange(sortIndex, 0, iNew + 1);
			System.arraycopy(sortIndex, 0, sortIndex, 1, iNew);
			sortIndex[0] = iNew;
			return false;
			}
			else
			{
			// compare living area to history
			//   find pages to compare to
			int min = 0, max = iLast, halfmin, halfmax, half, cellCount = history[iNew].cellCount;

			//System.out.println(" > cellCount = " + cellCount);
			
			if (cellCount > history[sortIndex[max]].cellCount)
				{
				//sort history page to the bottom if it is obvious
				//System.out.println(" > Sorted to top");
				//sortIndex = Arrays.copyOf(sortIndex, iNew + 1);
				sortIndex[iNew] = iNew;
				}
				else if (cellCount < history[sortIndex[min]].cellCount)
				{
				//sort history page to the top if it is obvious
				//System.out.println(" > Sorted to bottom");
				//sortIndex = Arrays.copyOf(sortIndex, iNew + 1);
				System.arraycopy(sortIndex, 0, sortIndex, 1, iNew);
				sortIndex[0] = iNew;
				}
				else
				{
				// *find pages to compare to
				halfmin = max;
				
				//System.out.println(" > min = " + min + ", halfmin = " + halfmin);
				
				if (history[sortIndex[min]].cellCount == cellCount)
					halfmin = min;
					else
					while((halfmin - min) > 1)
						{
						half = (halfmin + min) / 2;
						if (history[sortIndex[half]].cellCount < cellCount)
							min = half;
							else
							halfmin = half;
						}
				
				//System.out.println(" > min = " + min + ", halfmin = " + halfmin);
				//System.out.println(" > min cellCount = " + history[sortIndex[min]].cellCount
				//		+ ", halfmin cellCount= " + history[sortIndex[halfmin]].cellCount
				//		+ ", cellCount = " + cellCount);
				
				if (history[sortIndex[halfmin]].cellCount == cellCount)
					halfmax = halfmin;
					else
					{
					halfmax = halfmin - 1;
					max = halfmin;
					}
				
				//System.out.println(" > halfmax = " + halfmax + ", max = " + max);
				
				if (history[sortIndex[max]].cellCount == cellCount)
					halfmax = max;
					else
					while((max - halfmax) > 1)
						{
						half = (max + halfmax) / 2;
						if (cellCount < history[sortIndex[half]].cellCount)
							max = half;
							else
							halfmax = half;
						}
				
				//System.out.println(" > halfmax = " + halfmax + ", max = " + max);
				//System.out.println(" > halfmax cellCount = " + history[sortIndex[halfmax]].cellCount
				//		+ ", max cellCount= " + history[sortIndex[max]].cellCount
				//		+ ", cellCount = " + cellCount);
				//System.out.println(" > halfmin = " + halfmin + ", halfmax = " + halfmax);
				
				while (!(halfmin > halfmax))
					{
					//if (Arrays.deepEquals(history[sortIndex[halfmin]].livingArea, history[iNew].livingArea))
					if (livingAreasEquals(history[sortIndex[halfmin]], history[iNew]))
						{
						// if found
						//   calculate loopLength and go out (v)
						//System.out.println(" > found same!");
						individual.evaluation.loopLength = iNew - sortIndex[halfmin];
						return false;
						}
					
					halfmin++;
					}
				// sort
				int sortSpot;
					if (halfmin < halfmax) sortSpot = halfmax;
						else sortSpot = halfmin;
				//sortIndex = Arrays.copyOf(sortIndex, iNew + 1);
				System.arraycopy(sortIndex, sortSpot, sortIndex, sortSpot + 1, iNew - sortSpot);
				sortIndex[sortSpot] = iNew;
				}
			};
		// if not found
		//   increase cyclesLived by 1
		individual.evaluation.cyclesLived++;
		return true;
		}
	
	public EvaluatingGround(Individual individual)
		{
		this.individual = individual;
		}
	}
