package golEvolutioner;

import java.util.Arrays;

import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;

public class Population
	{
	Individual[] members;
	int[] sortIndex;
	
	public void draw(double size, Canvas display)
		{
		display.setWidth(32*(size + 1) + 1);
		display.setHeight(32*(size + 1) + 1);
		display.getGraphicsContext2D().setFill(Color.CADETBLUE);
		display.getGraphicsContext2D().fillRect(0, 0, display.getWidth(), display.getHeight());
		for (int x = 0; x < 32; x++)
			for (int y = 0; y < 32; y++)
				{
				int i = 32*y + x;
				//System.out.println("Drawing individual " + (32*x + y + 1) + "/1024");
				if (members[i] != null)
					members[i].draw(x, y, size, display);
				}
		
		}
	
	public void drawSorted(double size, Canvas display)
		{
		if ((sortIndex == null) || (sortIndex.length < 1024))
			{
			draw(size, display);
			return;
			}
		display.setWidth(32*(size + 1) + 1);
		display.setHeight(32*(size + 1) + 1);
		display.getGraphicsContext2D().setFill(Color.CADETBLUE);
		display.getGraphicsContext2D().fillRect(0, 0, display.getWidth(), display.getHeight());
		for (int x = 0; x < 32; x++)
			for (int y = 0; y < 32; y++)
				{
				int i = 32*y + x;
				//System.out.println("Drawing individual " + (32*x + y + 1) + "/1024");
				if (members[sortIndex[i]] != null)
					members[sortIndex[i]].draw(x, y, size, display);
				}
		
		}
	
	private double getFitness(int i)
		{
		if (members[i] == null)
			return -1;
			else if (members[i].evaluation == null)
			return 0;
			else
			return members[i].evaluation.fitness;
		}
	
	public void sort()
		{
		sortIndex = new int[]{0};
		int iLength = members.length;
		for (int i = 1; i < iLength; i++)
			{
			int min = 0, max = i - 1, half;
			double fitness = getFitness(i);	
			//System.out.println(" > fitness = " + fitness);
			if (fitness <= getFitness(sortIndex[max]))
				{
				//System.out.println(" > Sorted to top");
				sortIndex = Arrays.copyOf(sortIndex, i + 1);
				sortIndex[i] = i;
				}
				else if (fitness >= getFitness(sortIndex[min]))
				{
				//System.out.println(" > Sorted to bottom");
				sortIndex = Arrays.copyOf(sortIndex, i + 1);
				System.arraycopy(sortIndex, 0, sortIndex, 1, i);
				sortIndex[0] = i;
				}
				else
				{
				//System.out.println(" > min = " + min + ", max = " + max);
				while((max - min) > 1)
					{
					half = (max + min) / 2;
					if (getFitness(sortIndex[half]) > fitness)
						min = half;
						else if (getFitness(sortIndex[half]) < fitness)
						max = half;
						else
						{
						min = half;
						max = half;
						}
					}
				//System.out.println(" > min = " + min + ", max = " + max);
				//System.out.println(" > min cellCount = " + members[sortIndex[min]].evaluation.cellCount
				//		+ ", max cellCount= " + members[sortIndex[max]].evaluation.cellCount
				//		+ ", cellCount = " + cellCount);
				// sort
				sortIndex = Arrays.copyOf(sortIndex, i + 1);
				System.arraycopy(sortIndex, max, sortIndex, max + 1, i - max);
				sortIndex[max] = i;
				}
			}
		}
	}
