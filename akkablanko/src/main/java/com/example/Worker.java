// Alla Spitzer 222114
// Olha Borysova 230606
// Anastasiia Kulyani 230612
// Dmytro Pahuba 230665

package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.ArrayList;

public class Worker extends AbstractBehavior<Worker.Message> {

    public interface Message {};

    //liefert eine Zahl, die inkrementiert werden soll
    public static class Increment implements Message {
        int numberToIncrement;
        ActorRef<Worker.Message> multiplicator;

        public Increment(ActorRef<Worker.Message> multiplicator, int numberToIncrement){
            this.numberToIncrement = numberToIncrement;
            this.multiplicator = multiplicator;
        }
    }

    //liefert eine inkrementierte Zahl, die in die Liste hinzugefügt werden soll
    public static class Multiply implements Message {
        int incrementedNumber;
        public Multiply(int incrementedNumber){
            this.incrementedNumber = incrementedNumber;
        }
    }

    //liefert die Länge der Liste, die für inkrementierte Zahlen nötig ist
    public static class ListSize implements Message {
        int listSize;
        public ListSize(int listSize){
            this.listSize = listSize;
        }
    }

    public static Behavior<Message> create(ActorRef<Scheduler.Message> scheduler, ActorRef<Tasks.Message> task) {
        return Behaviors.setup(context -> new Worker(context, scheduler, task));
    }

    private ActorRef<Scheduler.Message> scheduler;
    private ActorRef<Tasks.Message> task;
    private ArrayList<Integer> incrementedNumbers;
    private int listSize;

    private Worker(ActorContext<Message> context, ActorRef<Scheduler.Message> scheduler, ActorRef<Tasks.Message> task) {
        super(context);
        this.scheduler = scheduler;
        this.task = task;
        this.incrementedNumbers = new ArrayList<>();
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(Increment.class, this::onIncrement)
                .onMessage(Multiply.class, this::onMultiply)
                .onMessage(ListSize.class, this::onListSize)
                .build();
    }

    //übergibt dem Multiplikator inkrementierte Zahl
    public Behavior<Message> onIncrement(Increment msg){
        msg.multiplicator.tell(new Worker.Multiply(msg.numberToIncrement + 1));
        this.scheduler.tell(new Scheduler.WorkerIsDone());
        return Behaviors.stopped();
    }

    //prüft, ob alle Zahlen inkrementiert worden sind und falls ja, berechnet das Produkt
    public Behavior<Message> onMultiply(Multiply msg){
        int result = 1;
        incrementedNumbers.add(msg.incrementedNumber);
        if (incrementedNumbers.size() == listSize){
            for (int val: incrementedNumbers) {
                result *= val;
            }
            task.tell(new Tasks.Result(result));
            this.scheduler.tell(new Scheduler.WorkerIsDone());
            return Behaviors.stopped();
        }

        return this;
    }

    //liefert die Länge der Liste, die für inkrementierte Zahlen nötig ist
    public Behavior<Message> onListSize(ListSize msg){
        listSize = msg.listSize;
        return this;
    }
}
