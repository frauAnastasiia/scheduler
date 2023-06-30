package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import java.util.Random;

import java.util.ArrayList;

public class Tasks extends AbstractBehavior<Tasks.Message> {

    public interface Message {};

    public static class CreateIncrementator implements Message {
        ActorRef<Worker.Message> worker;
        int numberToIncrement;

        private CreateIncrementator(ActorRef<Worker.Message> worker, int numberToIncrement){
            this.worker = worker;
            this.numberToIncrement = numberToIncrement;
        }
    }

    public static class CreateMultiplikator implements Message { }


    public static Behavior<Message> create(int id, ActorRef<Scheduler.Message> schedulerRef) {
        return Behaviors.setup(context -> new Tasks(context, id, schedulerRef);
    }

    private String taskName;
    private ArrayList<Integer> taskList;
    private ActorRef<Scheduler.Message> scheduler;

    private Tasks(ActorContext<Message> context, int id, ActorRef<Scheduler.Message> schedulerRef) {
        super(context);
        this.taskName = "Task " + id;
        taskList = createRandomList();
        this.scheduler = schedulerRef;
    }

    private ArrayList<Integer> createRandomList(){
        ArrayList<Integer> randomNumbersList = new ArrayList<>();
        Random random = new Random();
        int listLength = random.nextInt(7) + 4;
        for(int i = 0; i < listLength; i++){
            int randomNumber = random.nextInt(6) + 1;
            randomNumbersList.add(randomNumber);
        }
        this.scheduler.tell(new Scheduler.TaskIsCreated(taskList, taskName, this.getContext().getSelf()));
        return randomNumbersList;
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(CreateIncrementator.class, this::onIncrement)
                .build();
    }

    private Behavior<Message> onIncrement(CreateIncrementator msg) {
        msg.worker.tell(new Worker.Increment(msg.numberToIncrement));
        //getContext().getLog().info("I ({}) got a message: ExampleMessage({},{})", this.name, msg.someReference, msg.someString);
        return this;
    }
}
