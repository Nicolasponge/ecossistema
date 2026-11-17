import ecossistema.model.*;
import ecossistema.model.entity.*;
import ecossistema.model.generation.*;
/** Mostra, a cada minuto, a média do gene de velocidade de coelhos e lobos (1.00 = igual ao início). */
public class Evolucao {
  public static void main(String[] a){
    int seeds = a.length > 0 ? Integer.parseInt(a[0]) : 5;
    int minutos = a.length > 1 ? Integer.parseInt(a[1]) : 15;
    for(long seed=1;seed<=seeds;seed++){
      World w=new WorldGenerator(seed).generate(160,100);
      StringBuilder sb=new StringBuilder("seed "+seed+" vel coelho/lobo: ");
      for(int t=1;t<=minutos*60*60;t++){ w.update(1/60.0);
        if(t%(3*60*60)==0){
          Stats s=w.fotografar();
          sb.append(String.format(java.util.Locale.US,"%.2f/%.2f ", s.geneVelCoelhos(), s.geneVelLobos()));
        }
      }
      int maxGer=0;
      for(Entity e: w.getEntidades()) if(e instanceof Animal an) maxGer=Math.max(maxGer, an.getGeracao());
      System.out.println(sb+" | geracao max "+maxGer);
    }
  }
}
