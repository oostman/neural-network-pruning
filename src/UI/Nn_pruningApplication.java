package UI;


import logic.pruning_logic;

import com.vaadin.Application;
import com.vaadin.ui.Window;


@SuppressWarnings("serial")
public class Nn_pruningApplication extends Application {
	@Override
	public void init() {
		
		Window mainWindow = new Window("Nn_pruning Application");

		VaadinComposite mycomposite = new VaadinComposite(new pruning_logic());
		
		mainWindow.addComponent(mycomposite);
		mainWindow.getContent().setSizeFull();
		setMainWindow(mainWindow);
		setTheme("NN_PruningTheme");
	}

}
