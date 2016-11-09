import java.util.ArrayList;
import java.util.List;

public class Ttml {
  
  private final List<String> nodeEndsByRegion = new ArrayList<String>();
  
  private void traverseForStyle() {
    for (String entry : nodeEndsByRegion) {
      applyStyleToOutput(0);
      for (int i = 0; i < 10; ++i) {
        new Ttml();
      }
    }
  }
  
  private void applyStyleToOutput(int start) {
  }
  
}

