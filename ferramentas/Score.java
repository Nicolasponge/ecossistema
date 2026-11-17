import ecossistema.model.*;
import ecossistema.model.entity.*;
import ecossistema.model.generation.*;
public class Score {
  public static void main(String[] a){
    StringBuilder sb=new StringBuilder();
    for(long seed=1;seed<=5;seed++){
      World w=new WorldGenerator(seed).generate(160,100);
      int wolfEnd=-1, rabEnd=-1;
      for(int t=0;t<=600*60;t++){ w.update(1/60.0);
        if(wolfEnd<0 && w.count(EntityType.WOLF)==0) wolfEnd=t/60;
        if(rabEnd<0 && w.count(EntityType.RABBIT)==0) rabEnd=t/60; }
      sb.append(String.format("[lobo:%s coelho:%s] ", wolfEnd<0?"vivo":wolfEnd+"s", rabEnd<0?"vivo":rabEnd+"s"));
    }
    System.out.println(sb);
  }
}
