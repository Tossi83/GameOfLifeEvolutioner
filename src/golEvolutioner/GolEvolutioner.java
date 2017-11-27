package golEvolutioner;

import java.util.Arrays;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
//import javafx.scene.paint.Color; test breaking comment out
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class GolEvolutioner extends Application
	{
	private Population population;
	private EvaluatingGround[] evaluatingGrounds;
	public static EvolutionerStatus status = new EvolutionerStatus();

	private Label labelSpeeds = new Label(" this is speed label ");
	private Label labelStatus = new Label(" this is status label ");
	private VBox statusPanel = new VBox(labelStatus, labelSpeeds);
	private Button buttonTest = new Button("Test");
	private ToggleButton buttonHolder = new ToggleButton("Keep Running");
	private ToggleButton buttonMain = new ToggleButton("Initialize");
	private HBox bottomPanel = new HBox(buttonMain, buttonHolder, buttonTest, statusPanel);
	
    @SuppressWarnings("unchecked")
	private Series<Number, Number>[] progress = new Series[7];
	private NumberAxis naGeneration = new NumberAxis("Generation", 1, 2, 1);
	private NumberAxis naFitness = new NumberAxis("Fitness", 0, 1000, 1000);
	private LineChart<Number,Number> chartFitnessProgress = 
            new LineChart<Number,Number>(naGeneration, naFitness);
	
	private Canvas display = new Canvas();
	private Pane displayWrap = new Pane(display);
	
	private BorderPane layout = new BorderPane(displayWrap, null, chartFitnessProgress, bottomPanel, null);
	private Scene scene = new Scene(layout);
	private Stage primaryStage;
    //public BorderPane(Node center, Node top, Node right, Node bottom, Node left)
	
	private double
		centerRegionBorder = 5,
		mouseBindX, mouseBindY;
	
	private void breedFirstBatch()
		{
		population = new Population();
		population.members = new Individual[1024];
		for (int i = 0; i < population.members.length; i++)
			population.members[i] = new Individual();
		population.draw(status.size, display);
		}
	
	private Timeline timelineEvaluationEnded = new Timeline
			(
			new KeyFrame
				(
				Duration.millis(1),
				new EventHandler<ActionEvent>()
					{
					public void handle(ActionEvent event)
						{
						population.draw(status.size, display);
						population.sort();
						progress[0].getData().add(new XYChart.Data<Number, Number>(
								status.generation, population.members[population.sortIndex[0]].evaluation.fitness));
						progress[1].getData().add(new XYChart.Data<Number, Number>(
								status.generation, population.members[population.sortIndex[171]].evaluation.fitness));
						progress[2].getData().add(new XYChart.Data<Number, Number>(
								status.generation, population.members[population.sortIndex[341]].evaluation.fitness));
						progress[3].getData().add(new XYChart.Data<Number, Number>(
								status.generation, population.members[population.sortIndex[511]].evaluation.fitness));
						progress[4].getData().add(new XYChart.Data<Number, Number>(
								status.generation, population.members[population.sortIndex[683]].evaluation.fitness));
						progress[5].getData().add(new XYChart.Data<Number, Number>(
								status.generation, population.members[population.sortIndex[853]].evaluation.fitness));
						progress[6].getData().add(new XYChart.Data<Number, Number>(
								status.generation, population.members[population.sortIndex[1023]].evaluation.fitness));
						naGeneration.setUpperBound(status.generation + 1);
						naGeneration.setLowerBound(0);
						naGeneration.setTickUnit(Math.ceil((status.generation + 1)/5d));
						naFitness.setUpperBound(
								population.members[population.sortIndex[0]].evaluation.fitness +
								population.members[population.sortIndex[0]].evaluation.fitness/50);
						naFitness.setLowerBound(0 - population.members[population.sortIndex[0]].evaluation.fitness/50);
						naFitness.setTickUnit(population.members[population.sortIndex[0]].evaluation.fitness/10);
						population.drawSorted(status.size, display);
						buttonMain.setText("Conduct selection");
						buttonMain.setSelected(false);
						}
					}
				)
			);
	
	private Timeline statusDisplay = new Timeline
			(
			new KeyFrame
				(
				Duration.millis(1000),
				new EventHandler<ActionEvent>()
					{
					public void handle(ActionEvent event)
						{
						int threadsCount = 0, totalSpeed =0;
						String speedText = "Threads speed: ";
						if (evaluatingGrounds != null)
							for (int i = 0; i < evaluatingGrounds.length; i++)
								if ((evaluatingGrounds[i] != null) && (evaluatingGrounds[i].isRunning))
									{
									threadsCount++;
									int momentSpeed = evaluatingGrounds[i].speedThread;
									totalSpeed = totalSpeed + momentSpeed;
									speedText = speedText + momentSpeed;
									speedText = speedText + " ";
									}
						labelStatus.setText("Generation: " + status.generation + ","
								+ " evaluations started: " + status.evaluations + "/1024,"
								+ " evaluation threads running: " + threadsCount + "/" + status.evaluationThreadsCount);
						speedText = speedText + "cycles/s, "
								+ "total speed: " + totalSpeed + " cycles/s.";
						labelSpeeds.setText(speedText);
						if (buttonHolder.isSelected()) actionButtonMain();
						}
					}
				)
			);
	
	
	class ThreadEvaluationMain extends Thread
		{
		
		@Override
		public void run()
			{
			//System.out.println("Main evaluation thread started ...");
			evaluatingGrounds = new EvaluatingGround[status.evaluationThreadsCount];
			int j = 0;
			for (Individual individual : population.members)
				{
				if (individual.evaluation == null)
					{
					if (status.isShuttingDown) return;
					int i = 0;
					while ((evaluatingGrounds[i] != null) && (evaluatingGrounds[i].isRunning))
						{
						if (status.isShuttingDown) return;
						//System.out.println("Evaluating thread " + i + " is busy!");
						i++;
						if (i >= status.evaluationThreadsCount)
							{
							i = 0;
							try	{Thread.sleep(10);} catch (InterruptedException e) {}
							}
						}
					//System.out.println("Evaluating task: " + j + ", in thread: " + (i + 1));
					evaluatingGrounds[i] = new EvaluatingGround(individual);
					evaluatingGrounds[i].isRunning = true;
					evaluatingGrounds[i].startEvaluation();
					}
				j++;
				status.evaluations = j;
				if (status.isShuttingDown) return;
				}
			int tasksUnfinished = status.evaluationThreadsCount;
			while (tasksUnfinished > 0)
				{
				try	{Thread.sleep(1000);} catch (InterruptedException e) {}
				tasksUnfinished = 0;
				for (int i = 0; i < status.evaluationThreadsCount; i++)
					if ((evaluatingGrounds[i] != null) && (evaluatingGrounds[i].isRunning))
						tasksUnfinished++;
				//System.out.println("Tasks unfinished: " + tasksUnfinished);
				}
			timelineEvaluationEnded.play();
			status.step = 3;
			//System.out.println("Main evaluation thread ended ...");
			}
		}
	
	private void conductEvaluation()
		{
		ThreadEvaluationMain threadEvaluationMain = new ThreadEvaluationMain();
		threadEvaluationMain.start();
		}

	private void conductSelection()
		{
		for (int i = 960; i < 1024; i++)
			population.members[population.sortIndex[i]] = null;
		int erasedCount = 0, i = 959;
		while (erasedCount < 448)
			{
			double eraseChance = i - 64;
			eraseChance = (eraseChance/1790) + 0.25;
			if ((population.members[population.sortIndex[i]] != null) && (Math.random() < eraseChance))
				{
				population.members[population.sortIndex[i]] = null;
				erasedCount++;
				}
			i--;
			if (i < 64) i = 959;
			}
		population.drawSorted(status.size, display);
		}
	
	private void breed(int male, int female, int child)
		{
		int
			childWidth, childHeight,
			lowWidth, lowHeight,
			mOffsetX, mOffsetY, fOffsetX, fOffsetY;
		
		if (population.members[male].width > population.members[female].width) 
			{
			childWidth = population.members[male].width;
			lowWidth = population.members[female].width;
			mOffsetX = 0;
			fOffsetX = (int)(((double)(childWidth - population.members[female].width + 1)) * Math.random());
			}
			else
			{
			lowWidth = population.members[male].width;
			childWidth = population.members[female].width;
			mOffsetX = (int)(((double)(childWidth - population.members[male].width + 1)) * Math.random());
			fOffsetX = 0;
			}
		if (population.members[male].height > population.members[female].height) 
			{
			childHeight = population.members[male].height;
			lowHeight = population.members[female].height;
			mOffsetY = 0;
			fOffsetY = (int)(((double)(childHeight - population.members[female].height + 1)) * Math.random());
			}
			else
			{
			lowHeight = population.members[male].height;
			childHeight = population.members[female].height;
			mOffsetY = (int)(((double)(childHeight - population.members[male].height + 1)) * Math.random());
			fOffsetY = 0;
			}
		
		population.members[child] = new Individual();
		population.members[child].width = childWidth;
		population.members[child].height = childHeight;
		population.members[child].body = new boolean[childWidth][childHeight];
		
		double
			similiarity = ((double)lowWidth) / ((double)childWidth) * ((double)lowHeight) / ((double)childHeight);
			//sizeChange = (16/similiarity) * Math.random();
		
		for (int x = 0; x < population.members[male].width; x++)
			for (int y = 0; y < population.members[male].height; y++)
				population.members[child].body[x + mOffsetX][y + mOffsetY] = population.members[male].body[x][y];

		for (int x = 0; x < population.members[female].width; x++)
			for (int y = 0; y < population.members[female].height; y++)
				population.members[child].body[x + fOffsetX][y + fOffsetY] = population.members[female].body[x][y];
				
		for (int x = 0; x < lowWidth; x++)
			for (int y = 0; y < lowHeight; y++)
				{
				if (population.members[male].body[x + fOffsetX][y + fOffsetY] == population.members[female].body[x + mOffsetX][y + mOffsetY])
					population.members[child].body[x + mOffsetX + fOffsetX][y + mOffsetY + fOffsetY] = population.members[male].body[x + fOffsetX][y + fOffsetY];
					else
					{
					population.members[child].body[x + mOffsetX + fOffsetX][y + mOffsetY + fOffsetY] = (Math.random() < 0.5);
					similiarity = similiarity * 0.9;
					}
				}
		
		double sizeChange = (16/similiarity) * Math.random();
		if (sizeChange < 16)
			{
			if ((((int)sizeChange) & 1) > 0)
				{
				population.members[child].width++;
				population.members[child].body = Arrays.copyOf(population.members[child].body, population.members[child].width);
				population.members[child].body[population.members[child].width - 1] = new boolean[population.members[child].height];
				//System.out.println("Child grew to right");
				}
			if ((((int)sizeChange) & 2) > 0)
				{
				population.members[child].width++;
				population.members[child].body = Arrays.copyOf(population.members[child].body, population.members[child].width);
				System.arraycopy(population.members[child].body, 0, population.members[child].body, 1, population.members[child].width - 1);
				population.members[child].body[0] = new boolean[population.members[child].height];
				//System.out.println("Child grew to left");
				}
			if ((((int)sizeChange) & 4) > 0)
				{
				population.members[child].height++;
				for (int x = 0; x < population.members[child].width; x++)
					{
					population.members[child].body[x] = Arrays.copyOf(population.members[child].body[x], population.members[child].height);
					}
				//System.out.println("Child grew downward");
				}
			if ((((int)sizeChange) & 8) > 0)
				{
				population.members[child].height++;
				for (int x = 0; x < population.members[child].width; x++)
					{
					population.members[child].body[x] = Arrays.copyOf(population.members[child].body[x], population.members[child].height);
					System.arraycopy(population.members[child].body[x], 0, population.members[child].body[x], 1, population.members[child].height - 1);
					population.members[child].body[x][0] = false;
					}
				//System.out.println("Child grew upward");
				}
			}

		for (int x = 0; x < population.members[child].width; x++)
			for (int y = 0; y < population.members[child].height; y++)
				{
				int
					nCount = 0,
					xmin, xmax, ymin, ymax;
				
				if (x > 0) xmin = x - 1;
					else xmin = 0;
				if (y > 0) ymin = y - 1;
					else ymin = 0;
				if (x < population.members[child].width - 1) xmax = x + 2;
					else xmax = population.members[child].width;
				if (y < population.members[child].height - 1) ymax = y + 2;
					else ymax = population.members[child].height;
				
				for (int ix = xmin; ix < xmax; ix++)
					for (int iy = ymin; iy < ymax; iy ++)
						if (((ix != x) || (iy != y)) && (population.members[child].body[ix][iy])) nCount++;
				
				if (nCount > 0) population.members[child].body[x][y] = !(population.members[child].body[x][y] == (similiarity > Math.random()));
				}
				
		//System.out.println("similiarity = " + similiarity);
		//System.out.println("sizeChange = " + sizeChange);
		
		/*
		this.width = 1;
		this.height = 1;
		body = new boolean[this.width][this.height];
		body[0][0] = true;
		evaluation = null;
		*/
		}
	
	private void conductBreeding()
		{// TODO
		population.sort();
		for (int i = 0; i < 512; i++)
			{
			double r = Math.random();
			r = r*r;
			int j = (int)(512*r);
			breed(population.sortIndex[i], population.sortIndex[j], population.sortIndex[i + 512]);
			}
		population.drawSorted(status.size, display);
		}
		
	private double validateCanvasPositionX(double x, MouseEvent event)
		{
		double	areaWidth = displayWrap.getWidth(),
				canvasWidth = display.getWidth();
		
		if (areaWidth > canvasWidth + 2*centerRegionBorder)
			{
			if (x < centerRegionBorder)
				{
				x = centerRegionBorder;
				if (event != null) mouseBindX = event.getX();
				}
				else if (x + canvasWidth > areaWidth - centerRegionBorder)
				{
				x = areaWidth - canvasWidth - centerRegionBorder;
				if (event != null) mouseBindX = event.getX();
				}
			}
			else
			{
			if (x > centerRegionBorder)
				{
				x = centerRegionBorder;
				if (event != null) mouseBindX = event.getX();
				}
				else if (x + canvasWidth < areaWidth - centerRegionBorder)
				{
				x = areaWidth - canvasWidth - centerRegionBorder;
				if (event != null) mouseBindX = event.getX();
				}
			}
		return x;
		}
	
	private double validateCanvasPositionY(double y, MouseEvent event)
		{
		double	areaHeight = displayWrap.getHeight(),
				canvasHeight = display.getHeight();


		if (areaHeight > canvasHeight + 2*centerRegionBorder)
			{
			if (y < centerRegionBorder)
				{
				y = centerRegionBorder;
				if (event != null) mouseBindY = event.getY();
				}
				else if (y + canvasHeight > areaHeight - centerRegionBorder)
				{
				y = areaHeight - canvasHeight - centerRegionBorder;
				if (event != null) mouseBindY = event.getY();
				}
			}
			else
			{
			if (y > centerRegionBorder)
				{
				y = centerRegionBorder;
				if (event != null) mouseBindY = event.getY();
				}
				else if (y + canvasHeight < areaHeight - centerRegionBorder)
				{
				y = areaHeight - canvasHeight - centerRegionBorder;
				if (event != null) mouseBindY = event.getY();
				}
			}
		return y;
		}
	
	private void setupLayout()
		{/*
		naGeneration.setStyle(
				"-fx-tick-label-fill: #ffdfbf;" +
				"-fx-label-text-fill: #ffdfbf;" +
				"-fx-background-color: #ffdfbf;" +
				"-fx-effect: dropshadow( gaussian , rgba(0, 0, 0, 0.5) , 0, 0, 0, 1 );");*/
		//chartFitnessProgress.setStyle("-fx-background-color: #604020;-fx-text-fill: #ffdfbf;");
		for (int i = 0; i < 7; i++)
			{
			progress[i] = new Series<Number, Number>();
			chartFitnessProgress.getData().add(progress[i]);
			}
		chartFitnessProgress.setPrefWidth(100);
		
		bottomPanel.getStyleClass().add("bottomPanel");
		
		statusPanel.getStyleClass().add("statusPanel");
		HBox.setHgrow(statusPanel, Priority.ALWAYS);
		
		scene.getStylesheets().add("golEvolutioner/golevolutioner.css");
		
		primaryStage.setTitle(status.title);
		primaryStage.setMinWidth(640);
		primaryStage.setMinHeight(480);
		
		statusDisplay.setCycleCount(Timeline.INDEFINITE);
		statusDisplay.play();
		
		displayWrap.widthProperty().addListener
			(
			(obs, oldValue, newValue) -> 
				{
				display.relocate(validateCanvasPositionX(display.getLayoutX(), null), display.getLayoutY());
				//System.out.println("bzzz");
				chartFitnessProgress.setPrefWidth(scene.getWidth()*0.34375);
				//System.out.println("prefW: " + chartFitnessProgress.getPrefWidth());
				//System.out.println("prefW: " + chartFitnessProgress.getWidth());
				}
	    	);
		
		displayWrap.heightProperty().addListener
			(
			(obs, oldValue, newValue) -> 
				{
				display.relocate(display.getLayoutX(), validateCanvasPositionY(display.getLayoutY(), null));
				}
			);

		buttonMain.setOnAction
			(
			new EventHandler<ActionEvent>()
				{
				@Override
				public void handle(ActionEvent event)
					{
					actionButtonMain();
					}
				}
			);
		
		buttonTest.setOnAction
			(
			new EventHandler<ActionEvent>()
				{
				@Override
				public void handle(ActionEvent event)
					{
					actionButtonTest();
					}
				}
			);
		
		displayWrap.setOnScroll
			(
			new EventHandler<ScrollEvent>()
				{
				@Override
				public void handle(ScrollEvent event)
					{
					if (event.getDeltaY() > 0)
						{
						if (status.size < 128) status.size = status.size + 8;
						}
						else
						{
						if (status.size > 16) status.size = status.size - 8;
						}
					if (population != null)
						{
						population.drawSorted(status.size, display);
						display.relocate
							(
							validateCanvasPositionX(display.getLayoutX(), null),
							validateCanvasPositionY(display.getLayoutY(), null)
							);
						}
					}
				}
			);
		
		display.setOnMouseClicked
			(
			new EventHandler<MouseEvent>()
				{
				@Override
				public void handle(MouseEvent event)
					{
					if (event.isStillSincePress())
						{
						/*
						int cellSize = lsEditable.getCellSize() + 1,
						mouseX = (int) event.getX(), mouseY = (int) event.getY();
						if ((mouseX % cellSize != 0) && (mouseY % cellSize != 0))
							lsEditable.alterCell(mouseX/cellSize, mouseY/cellSize); 
						 */
						}
					}
				}
			);
		
		display.setOnMousePressed
			(
			new EventHandler<MouseEvent>()
				{
				@Override
				public void handle(MouseEvent event)
					{
					mouseBindX = event.getX();
					mouseBindY = event.getY();
					}
				}
			);
		
		display.setOnMouseDragged
			(
			new EventHandler<MouseEvent>()
				{
				@Override
				public void handle(MouseEvent event)
					{
					display.relocate
						(
						validateCanvasPositionX(display.getLayoutX()  + event.getX() - mouseBindX, event),
						validateCanvasPositionY(display.getLayoutY()  + event.getY() - mouseBindY, event)
						);
					}
				}
			);
		}
	
	private void resizeButtons()
		{/*
		double newWidth = 0;
		for (Object child : buttons.getChildren())
			{
			if ((child instanceof ToggleButton) && (((ToggleButton)child).getWidth() > newWidth))
				newWidth = ((ToggleButton) child).getWidth();
			if ((child instanceof Button) && (((Button)child).getWidth() > newWidth))
				newWidth = ((Button) child).getWidth();				
			}*/
		
		for (Object child: bottomPanel.getChildren())
			{
			if (child instanceof ToggleButton)
				((ToggleButton) child).setMinWidth(112);
			if (child instanceof Button)
				((Button) child).setMinWidth(112);
			}
		
		//System.out.println("buttons width: " + newWidth);
		}
	
	private void actionButtonMain()
		{
		switch (status.step)
			{
			case 0:
				breedFirstBatch();
				buttonMain.setText("Evaluate first ones");
				buttonMain.setSelected(false);
				status.step = 1;
				//resizeButtons();
				break;
			case 1:
				status.step = 2;
				conductEvaluation();
				buttonMain.setText("evaluating...");
				//resizeButtons();
				break;
			case 2:
				buttonMain.setSelected(true);
				//resizeButtons();
				break;
			case 3:
				conductSelection();
				buttonMain.setText("Mate new batch");
				status.step = 4;
				buttonMain.setSelected(false);
				//resizeButtons();
				break;
			case 4:
				conductBreeding();
				status.generation++;
				buttonMain.setText("Evaluate");
				status.step = 1;
				buttonMain.setSelected(false);
				//resizeButtons();
				break;
			default: break;
			}
		}
	
	private EvaluatingGround eG;
	//private int testswitch = 12;
	private ThreadTest12 threadTest12;
	
	private void actionButtonTest()
		{
		System.out.println("Test (" + status.testSwitch + ") =======================================================");
		switch (status.testSwitch)
			{
			case 1: // display basic stuff about test individual
				if (population == null) breedFirstBatch();
				if (eG == null)
					{
					population.members[0].randomize(10);
					eG = new EvaluatingGround(population.members[0]);
					eG.isRunning = true;
					eG.startEvaluation();
					}
				else
					eG.doLifeStep_public();
				
				//int i1eGi = eG.history.length - 1; test breaking comment out
				for (int x = 0; x < 256; x++)
					for (int y = 0; y < 256; y++)
						{/* test breaking comment out
						if (eG.history[i1eGi].livingArea[x][y])
							{
							display.getGraphicsContext2D().setFill(Color.WHITE);
							}
							else
							{
							display.getGraphicsContext2D().setFill(Color.BLACK);
							}*/
						display.getGraphicsContext2D().fillRect(x, y, 1, 1);
						}

				System.out.println
					(
					"Test subject: dimensions = "
					+ population.members[0].width + "x" + population.members[0].height
					+ ", size = " + population.members[0].evaluation.size
					+ ", cells = " + population.members[0].evaluation.cellCount
					);
				break;
			case 2: //evaluate sample individual
				population.members[0].randomize(10);
				
				eG = new EvaluatingGround(population.members[0]);
				eG.isRunning = true;
				eG.startEvaluation();
				
				while(eG.isRunning);
				
				population.draw(32, display);
				System.out.println
					(
					"Test subject: dimensions = "
					+ population.members[0].width + "x" + population.members[0].height
					+ ", size = " + population.members[0].evaluation.size
					+ ", cells = " + population.members[0].evaluation.cellCount
					+ ", cycles lived = " + population.members[0].evaluation.cyclesLived
					+ ", loop length = " + population.members[0].evaluation.loopLength
					+ ", fitness = " + population.members[0].evaluation.fitness
					+ "."
					);
				break;
			case 3: //testing speed of population drawing
				for (int i = 0; i < 1024; i++)
					{
					System.out.println("Randomizing individual " + i);
					population.members[i].randomize(64);
					}
				//conductEvaluation();

				population.draw(256, display);
				break;
			case 4: // testing drawing of individuals
				population.members[1].randomize(10);
				population.draw(32, display);
				break;
			case 5: // testing speed of comparison statements
				int i = 0, j = 0, t = 3;
				@SuppressWarnings("unused") boolean b;
				long timeMark = System.nanoTime();
				System.out.println("Comparison through less and greater.");
				while (j < 10)
					{
					t = (int)(Math.random()*4 + 1);
					i++;
					b = ((1 < t) && (t < 4));
					if (System.nanoTime() - timeMark > 1000000000)
						{
						timeMark = timeMark + 1000000000;
						System.out.println("Test speed: " + i + "/s");
						i = 0;
						j++;
						}
					}
				System.out.println("Comparison through equality.");
				j = 0;
				while (j < 10)
					{
					t = (int)(Math.random()*4 + 1);
					i++;
					b = ((t == 2) || (t == 3));
					if (System.nanoTime() - timeMark > 1000000000)
						{
						timeMark = timeMark + 1000000000;
						System.out.println("Test speed: " + i + "/s");
						i = 0;
						j++;
						}
					}
				System.out.println("Test end.");
				break;
			case 6: // testing speed of memory manipulation
				HistoryPage[] thistory = new HistoryPage[0];
				long timeMark2 = System.nanoTime();
				for (int i2 = 0; i2 < 1000000; i2++)
					{
					thistory = Arrays.copyOfRange(thistory, 0, i2 + 1);
					thistory[i2] = new HistoryPage();
					//thistory[i2].livingArea[0][0] = true; test breaking comment out
					if (System.nanoTime() - timeMark2 > 1000000000)
						{
						timeMark2 = timeMark2 + 1000000000;
						thistory[i2] = new HistoryPage();
						System.out.println
							("Test step: " + i2
							+ ", page test: cellCount = " + thistory[i2].cellCount
							/*+ ", cellValue = " + thistory[0].livingArea[0][0] test breaking comment out*/);
						}
					}
				break;
			case 7: // ackerman function just for fun
				int i7min = 0, i7max = 0;
				boolean b7run = true;
				while (b7run)
					{
					if (i7min == i7max)
						{
						System.out.println("ack(" + i7min + ", " + i7max + ") = " + ack(i7min, i7max));
						i7min = 0;
						i7max++;
						}
						else if (i7min < i7max)
						{
						System.out.println("ack(" + i7min + ", " + i7max + ") = " + ack(i7min, i7max));
						System.out.println("ack(" + i7max + ", " + i7min + ") = " + ack(i7max, i7min));
						i7min++;
						}
						else b7run = false;
					}
				break;
			case 8: // some unique iteration test
				int a8 = 1, b8 = 1, temp8;
				for (int i8 = 0; i8 < 1024; i8++)
					{
					System.out.println("step " + i8 + ": " + a8 + ", " + b8);
					if (a8 == b8)
						{
						a8 = 1;
						b8++;
						}
						else if (a8 < b8)
						{
						temp8 = a8;
						a8 = b8;
						b8 = temp8;
						}
						else
						{
						temp8 = a8;
						a8 = b8 + 1;
						b8 = temp8;
						}
					}
				break;
			case 9: // testing array dynamics
				HistoryPage[] thistory9 = new HistoryPage[0];
				for (int i9 = 0; i9 < 10; i9++)
					{
					thistory9 = Arrays.copyOf(thistory9, i9 + 1);
					System.arraycopy(thistory9, 0, thistory9, 1, i9);
					thistory9[0] = new HistoryPage();
					thistory9[0].cellCount = (int)(Math.random() * 10);
					String s9 = "Array " + i9 + ": ";
					for (int j9 = 0; j9 < thistory9.length; j9++)
						s9 = s9 + thistory9[j9].cellCount + ", ";
					System.out.println(s9);
					}
				break;
			case 10: // for testing lifestep and it's sorting functions
				if (population == null) breedFirstBatch();
				if (eG == null)
					{
					population.members[0].randomize(10);
					eG = new EvaluatingGround(population.members[0]);
					eG.isRunning = true;
					eG.startEvaluation();
					}
				else
					eG.doLifeStep_public();
				
				int i10eGi = eG.history.length - 1;
				for (int x = 0; x < 256; x++)
					for (int y = 0; y < 256; y++)
						{/* test breaking comment out
						if (eG.history[i10eGi].livingArea[x][y])
							{
							display.getGraphicsContext2D().setFill(Color.WHITE);
							}
							else
							{
							display.getGraphicsContext2D().setFill(Color.BLACK);
							}*/
						display.getGraphicsContext2D().fillRect(x, y, 1, 1);
						}

				System.out.println
					(
					"Test subject: dimensions = "
					+ population.members[0].width + "x" + population.members[0].height
					+ ", size = " + population.members[0].evaluation.size
					+ ", cells = " + population.members[0].evaluation.cellCount
					);

				i10eGi++;
				String s10 = "Cell counts array: ";
				for (int j10 = 0; j10 < i10eGi; j10++)
					s10 = s10 + eG.history[j10].cellCount + ", ";
				System.out.println(s10);
				s10 = "Sort index array: ";
				for (int j10 = 0; j10 < i10eGi; j10++)
					s10 = s10 + eG.sortIndex[j10] + ", ";
				System.out.println(s10);
				s10 = "Sorted cell counts array: ";
				for (int j10 = 0; j10 < i10eGi; j10++)
					s10 = s10 + eG.history[eG.sortIndex[j10]].cellCount + ", ";
				System.out.println(s10);
				System.out.println
					("i10eGi = " + i10eGi
					+ ", sort index array length = " + eG.sortIndex.length
					+ ", history length = " +  eG.history.length);
				break;
			case 11: // testing population sorting
				if (population == null) breedFirstBatch();
				for (int i11 = 0; i11 < 1024; i11++)
					{
					if (population.members[i11].evaluation == null)
						population.members[i11].evaluation = new EvaluationData();
					population.members[i11].evaluation.fitness = ((double)((int)(Math.random()*10000)))/100;
					}
				population.sort();
				
				String s11 = "Fitness array: ";
				for (int j11 = 0; j11 < 1024; j11++)
					s11 = s11 + population.members[j11].evaluation.fitness + ", ";
				System.out.println(s11);
				s11 = "Sort index array: ";
				for (int j11 = 0; j11 < 1024; j11++)
					s11 = s11 + population.sortIndex[j11] + ", ";
				System.out.println(s11);
				s11 = "Sorted fitness array: ";
				for (int j11 = 0; j11 < 1024; j11++)
					s11 = s11 + population.members[population.sortIndex[j11]].evaluation.fitness + ", ";
				System.out.println(s11);
				boolean b11 = true;
				for (int j11 = 1; j11 < 1024; j11++)
					if (population.members[population.sortIndex[j11-1]].evaluation.fitness < population.members[population.sortIndex[j11]].evaluation.fitness)
						b11 = false;
				if (b11)
					System.out.println("Sorted properly!");
					else
					System.out.println("errors in sorting");
				population.drawSorted(32, display);
				status.testSwitch = 3;
				break;
			case 12:
				buttonTest.setDisable(true);
				if (population == null) breedFirstBatch();
				for (int i12 = 0; i12 < 1024; i12++) population.members[i12].randomize(3);
				population.sort();
				population.draw(64, display);
				threadTest12 = new ThreadTest12();
				threadTest12.start();
				break;
			case 13:
				if (status.step == 3)
					{
					for (int i13 = 0; i13 < 1024; i13++)
						if ((population.members[i13] != null) && (population.members[i13].evaluation != null))
							population.members[i13].evaluation.fitness = ((double)((int)(Math.random() * 10000))) / 100;
					population.sort();
					population.drawSorted(32, display);
					}
				else System.out.println("Status step is: " + status.step);
				break;
			case 14:
				if (population == null) breedFirstBatch();
				if (population.members == null)	population.members = new Individual[1024];
				if (population.members[0] == null) population.members[0] = new Individual();
				if (population.members[1] == null) population.members[1] = new Individual();
				population.members[0].randomize(16);
				population.members[1].randomize(16);
				breed(0, 1, 2);
				population.draw(64, display);
				break;
			case 15:
				if (population == null) breedFirstBatch();
				if (population.members == null)	population.members = new Individual[1024];
				if (population.members[0] == null) population.members[0] = new Individual();
				if (population.members[1] == null) population.members[1] = new Individual();
				population.members[0].width = 16;
				population.members[0].height = 8;
				population.members[0].body = new boolean[16][8];
				for (int x15 = 0; x15 < 16; x15++)
					for (int y15 = 0; y15 < 8; y15++)
						population.members[0].body[x15][y15] = ((y15 & 1) == 0);
				population.members[1].width = 8;
				population.members[1].height = 16;
				population.members[1].body = new boolean[8][16];
				for (int x15 = 0; x15 < 8; x15++)
					for (int y15 = 0; y15 < 16; y15++)
						population.members[1].body[x15][y15] = ((x15 & 1) == 0);
				breed(0, 1, 2);
				population.draw(64, display);
				break;
			case 16:
				long startTime16 = System.nanoTime();
				for(int i16 = 0; i16 < 100000000; i16++)
					{
					@SuppressWarnings("unused")
					long test = System.nanoTime();
					}
				long endTime16 = System.nanoTime();

				double speed = 100000000000000000d/((double)(endTime16-startTime16));
				System.out.println("nanoTime speed: " + speed + "/s");
				
				startTime16 = System.currentTimeMillis();
				for(int i16 = 0; i16 < 100000000; i16++)
					{
					@SuppressWarnings("unused")
					long test = System.currentTimeMillis();
					}
				endTime16 = System.currentTimeMillis();
				
				speed = 100000000000d/((double)(endTime16-startTime16));
				System.out.println("currentTimeMillis speed: " + speed + "/s");
				break;
			case 17:
				for (String styleClass : layout.getStyleClass())
					{
					System.out.println("getStyleClass: " + styleClass);
					}
				break;
			case 18:
				progress[0].getData().add(new XYChart.Data<Number, Number>(1, 23d));
				progress[0].getData().add(new XYChart.Data<Number, Number>(2, 740d));
				break;
			case 19:
				int i19a = 4, i19b = 4;
				if (i19a == 3)
					if (i19b == 4)	
						System.out.println("Loop 1");
					else if (i19b == 4)
						System.out.println("Loop 2");
				break;
			}
		}
	
	class ThreadTest12 extends Thread
		{
		@Override
		public void run()
			{
			status.step = 2;
			conductEvaluation();
			while (status.step == 2) try {Thread.sleep(1000);} catch (InterruptedException e) {}
			System.out.println("After evaluation");
			population.sort();
			System.out.println("After sorting");
			population.drawSorted(64, display);
			System.out.println("After sorted draw");
			buttonTest.setDisable(false);
			}
		}
	
	private int ack(int m, int n)
		{
		System.out.println("executing ack(" + m + ", " + n + ")");
		if (m == 0)
			return n + 1;
			else if (n == 0)
			return ack(m - 1, 1);
			else
			return ack(m - 1, ack(m, n - 1));
		}
	
	@Override
	public void start(Stage primaryStage) throws Exception
		{
		this.primaryStage = primaryStage;
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest
			(
			new EventHandler<WindowEvent>()
				{
				@Override
				public void handle(WindowEvent event)
					{
					status.isShuttingDown = true;
					}
				}
			);

		setupLayout();
		primaryStage.show();
		resizeButtons();
		}

	public static void main(String[] args)
		{
		launch(args);
		}
	}
