// Alla Spitzer 222114
// Olha Borysova 230606
// Anastasiia Kulyani 230612
// Dmytro Pahuba 230665

package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import java.util.Random;

import java.util.ArrayList;

public class Tasks extends AbstractBehavior<Tasks.Message> {

    public interface Message {};

    //liefert Liste mit Referenzen auf Workers
    public static class CreatedWorkers implements Message {
        ArrayList<ActorRef<Worker.Message>> neededWorkers;

        public CreatedWorkers(ArrayList<ActorRef<Worker.Message>> neededWorkers){
            this.neededWorkers = neededWorkers;
        }
    }

    //liefert das endliche Ergebnis nach der Multiplikation
    public static class Result implements Message {
        int result;

        public Result(int result){
            this.result = result;
        }
    }


    public static Behavior<Message> create(int id, ActorRef<Scheduler.Message> schedulerRef) {
        return Behaviors.setup(context -> new Tasks(context, id, schedulerRef));
    }

    private String taskName;
    private ArrayList<Integer> taskList;
    private ActorRef<Scheduler.Message> scheduler;

    private Tasks(ActorContext<Message> context, int id, ActorRef<Scheduler.Message> schedulerRef) {
        super(context);
        this.taskName = "Task " + id;
        this.scheduler = schedulerRef;
        taskList = createRandomList();
    }

    //erstellt eine Liste mit zufällig generierten ganzen Zahlen zwischen 1 und 6
    private ArrayList<Integer> createRandomList(){
        ArrayList<Integer> randomNumbersList = new ArrayList<>();
        Random random = new Random();
        int listLength = random.nextInt(7) + 4;
        for(int i = 0; i < listLength; i++){
            int randomNumber = random.nextInt(6) + 1;
            randomNumbersList.add(randomNumber);
        }
        this.scheduler.tell(new Scheduler.TaskIsCreated(taskName, this.getContext().getSelf(), randomNumbersList.size() + 1));
        return randomNumbersList;
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(CreatedWorkers.class, this::onCreatedWorkers)
                .onMessage(Result.class, this::onResult)
                .build();
    }

    //ordnet die Aufgaben den erstellten Workern zu
    private Behavior<Message> onCreatedWorkers(CreatedWorkers msg) {
        ActorRef<Worker.Message> multiplicator = msg.neededWorkers.get(msg.neededWorkers.size() - 1);
        multiplicator.tell(new Worker.ListSize(this.taskList.size()));
        getContext().getLog().info("List:" + taskList.toString());
        for (int i = 0; i < taskList.size(); i++){
            msg.neededWorkers.get(i).tell(new Worker.Increment(multiplicator, taskList.get(i)));
        }
        return this;
    }

    //liefert das Ergebnis nach der Multiplikation auf die Konsole
    private Behavior<Message> onResult(Result msg){
        getContext().getLog().info("Result of multiplication for {} is: {}", this.taskName, msg.result);
        return this;
    }
}
