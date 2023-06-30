package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import java.util.Random;

import java.util.ArrayList;

public class Worker extends AbstractBehavior<Worker.Message> {

    public interface Message {};

    public static class Increment implements Message {
        int number;
        public Increment(int number){
            this.number = number;
        }
    }

    public static Behavior<Message> create(ActorRef<Scheduler.Message> scheduler) {
        return Behaviors.setup(context -> new Worker(context, scheduler));
    }

    private ActorRef<Scheduler.Message> scheduler;

    private Worker(ActorContext<Message> context, ActorRef<Scheduler.Message> scheduler) {
        super(context);
        this.scheduler = scheduler;
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(Increment.class, this::onIncrement)
                .build();
    }

    public Behavior<Message> onIncrement(Increment msg){
        msg.number += 1;
        //message dass er fertig ist
        this.scheduler.tell();
        return Behaviors.stopped();
    }
}
