package avilaos.ui;
import avilaos.ui.session.*;
public interface UiView {void render(SessionView view);void showNotice(SessionNotice notice);void closed();}
