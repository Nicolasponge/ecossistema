import ecossistema.model.*;
import ecossistema.model.entity.*;
import ecossistema.model.generation.*;
/** Mostra coelhos/lobos/arbustos a cada 30s, para ver as oscilações sem abrir a janela. */
public class Timeline {
  public static void main(String[] a){
    int seeds = a.length > 0 ? Integer.parseInt(a[0]) : 5;
    int minutos = a.length > 1 ? Integer.parseInt(a[1]) : 10;
    int sobreviveu = 0;
    for(long seed=1;seed<=seeds;seed++){
      World w=new WorldGenerator(seed).generate(160,100);
      StringBuilder sb=new StringBuilder("seed "+seed+" (coelho/lobo): ");
      int minR=Integer.MAX_VALUE, maxR=0, minW=Integer.MAX_VALUE, maxW=0;
      for(int t=1;t<=minutos*60*60;t++){ w.update(1/60.0);
        if(t%(30*60)==0){
          int r=w.count(EntityType.RABBIT), l=w.count(EntityType.WOLF);
          sb.append(r).append('/').append(l).append(' ');
          if(t>=120*60){ minR=Math.min(minR,r); maxR=Math.max(maxR,r); minW=Math.min(minW,l); maxW=Math.max(maxW,l); }
        }
      }
      boolean ok = w.count(EntityType.RABBIT)>0 && w.count(EntityType.WOLF)>0;
      if(ok) sobreviveu++;
      System.out.println(sb + String.format(" | depois de 2min: coelho %d..%d lobo %d..%d %s", minR,maxR,minW,maxW, ok?"OK":"MORREU"));
    }
    System.out.println("sobreviveram: "+sobreviveu+"/"+seeds);
  }
}
