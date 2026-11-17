import ecossistema.model.*;
import ecossistema.model.entity.*;
import ecossistema.model.generation.*;
public class Clump {
  public static void main(String[] a){
    double soma=0; int amostras=0, pior=0;
    for(long seed=1;seed<=3;seed++){
      World w=new WorldGenerator(seed).generate(160,100);
      for(int t=1;t<=150*60;t++){ w.update(1/60.0);
        if(t%(5*60)==0){ for(Entity e:w.getEntidades()) if(e instanceof Rabbit r){ int c=0; for(Entity o:w.neighbors(r,1.3)) if(o instanceof Rabbit) c++; soma+=c; amostras++; pior=Math.max(pior,c);} } }
    }
    System.out.printf("coelhos vizinhos (raio 1.3 tile = 1 sprite): media=%.2f  pior=%d%n", soma/amostras, pior);
  }
}
