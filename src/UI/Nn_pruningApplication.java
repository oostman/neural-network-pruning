package UI;


import logic.PruningController;

import com.vaadin.Application;
import com.vaadin.ui.Window;


@SuppressWarnings("serial")
public class Nn_pruningApplication extends Application {
	@Override
	public void init() {
		
		Window mainWindow = new Window("Nn_pruning Application");

		VaadinComposite mycomposite = new VaadinComposite(new PruningController());
		
		mainWindow.addComponent(mycomposite);
		mainWindow.getContent().setSizeFull();
		setMainWindow(mainWindow);
		setTheme("NN_PruningTheme");
	}

}
