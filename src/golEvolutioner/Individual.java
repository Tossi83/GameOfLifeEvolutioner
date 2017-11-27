package golEvolutioner;

import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class Individual
	{
	int width,
		height;
	boolean[][] body;
	EvaluationData evaluation;
	
	public Individual()
		{
		this.width = 1;
		this.height = 1;
		body = new boolean[this.width][this.height];
		body[0][0] = true;
		evaluation = null;
		}

	public void draw(int x, int y, double size, Canvas display)
		{
		display.getGraphicsContext2D().setFill(Color.WHEAT);
		display.getGraphicsContext2D().fillRect((size + 1)*x + 1, (size + 1)*y + 1, size, size);
		double tempSizeX, tempSizeY, tempSizeMax, cellSize;
		if (this.width > this.height)
			tempSizeMax = this.width;
			else
			tempSizeMax = this.height;
		cellSize = (size - 2)/tempSizeMax;
		tempSizeX = (size - 2 - cellSize*this.width)/2;
		tempSizeY = (size - 2 - cellSize*this.height)/2;
		for (int ix = 0; ix < width; ix++)
			for (int iy = 0; iy < height; iy++)
				{
				display.getGraphicsContext2D().setFill(Color.BLACK);
				display.getGraphicsContext2D().fillRect(
						(size + 1)*x + 2 + ix*cellSize + tempSizeX, (size + 1)*y + 2 + iy*cellSize + tempSizeY, cellSize, cellSize);
				if (body[ix][iy])
					{
					display.getGraphicsContext2D().setFill(Color.WHITE);
					display.getGraphicsContext2D().fillOval
							((size + 1)*x + 2 + ix*cellSize + tempSizeX, (size + 1)*y + 2 + iy*cellSize + tempSizeY, cellSize, cellSize);
					}
				}
		if (!(evaluation == null))
			{	
			display.getGraphicsContext2D().setFill(Color.GREEN);
			display.getGraphicsContext2D().setFont(new Font(size/4));
			display.getGraphicsContext2D().fillText("fitness: ", (size + 1)*x + 1, (size + 1)*y + size*0.25);
			display.getGraphicsContext2D().fillText("" + evaluation.fitness, (size + 1)*x + 1, (size + 1)*y + size*0.5);
			display.getGraphicsContext2D().fillText("eTime: ", (size + 1)*x + 1, (size + 1)*y + size*0.75);
			display.getGraphicsContext2D().fillText("" + evaluation.evaluationTime, (size + 1)*x + 1, (size + 1)*y + size);
			}
		}
	
	/*public double getFitness()
		{
		if (evaluation == null)
			return 0;
			else
			return evaluation.fitness;
		}*/
	
	public void randomize(int i)
		{
		i = (i & 255) + 1;
		width = 1 + ((int)(Math.random()*i));
		height = 1 + ((int)(Math.random()*i));
		double r = Math.random();
		//r = r*r*r;
		r = r*r;
		body = new boolean[this.width][this.height];
		for (int x = 0; x < this.width; x++)
			for (int y = 0; y < this.height; y++)
				body[x][y] = (r > Math.random());
		evaluation = null;
		}
	}
