package net.unknown.survival.gui.hopper.view;

import net.unknown.core.gui.view.View;
import net.unknown.survival.gui.hopper.ConfigureHopperGui;

public interface ConfigureHopperView extends View {
    ConfigureHopperGui getGui();

    ConfigureHopperViewBase getParentView();
}
