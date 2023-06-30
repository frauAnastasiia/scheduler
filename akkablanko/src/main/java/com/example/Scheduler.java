package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.TimerScheduler;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;


public class Scheduler extends AbstractBehavior<Scheduler.Message> {

    public interface Message {};


    public static class CreateTask implements Message {}

    public static class TaskIsCreated implements Message {
        ArrayList<Integer> taskList;
        String taskName;
        ActorRef<Tasks.Message> task;
        public TaskIsCreated(ArrayList<Integer> taskList, String taskName, ActorRef<Tasks.Message> task){
            this.taskList = taskList;
            this.taskName = taskName;
            this.task = task;
        }
    }

    public static Behavior<Message> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new Scheduler(context, timers)));
    }

    private static final int MAX_WORKERS = 20;
    private int activeWorkers;
    private final TimerScheduler<Scheduler.Message> timers;
    private Queue<ActorRef<Tasks.Message>> taskQueue;
    private ActorRef<Tasks.Message> task;
    private ActorRef<Worker.Message> worker;

    private Scheduler(ActorContext<Message> context, TimerScheduler<Scheduler.Message> timers) {
        super(context);
        this.timers = timers;
    }
        //Message msg = new ExampleMessage("test123");
        //this.timers.startSingleTimer(msg, msg, Duration.ofSeconds(10));

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(CreateTask.class, this::onCreateTask)
                .onMessage(TaskIsCreated.class, this::isCreated)
                .build();
    }

    private Behavior<Message> onCreateTask() {
        int countTasks = 1;
        while(countTasks <= 20){
            task = this.getContext().spawn(Tasks.create(countTasks, this.getContext().getSelf()), "task " + countTasks);
            countTasks++;
        }
        return this;
    }

    private Behavior<Message> isCreated(ArrayList<Integer> taskList, String taskName, ActorRef<Tasks.Message> task) {
        if(activeWorkers < MAX_WORKERS){
            activeWorkers += taskList.size() + 1;
            for (int i = 0; i < activeWorkers - 1; i++) {
                ActorRef<Worker.Message> newWorker = this.getContext().spawn(Worker.create(this.getContext().getSelf()), "worker");
                task.tell(new Tasks.CreateIncrementator(newWorker, taskList.get(i)));
                //передаем в таскс нового воркера, чтобы он делал свою задачу
            }

            //нужно по идее передать сюда новый лист с инкрементированными значениями
            //ActorRef<Worker.Message> newWorker = this.getContext().spawn(Worker.create(this.getContext().getSelf()), "worker");
            //task.tell(new Tasks.CreateMultiplikator(newWorker, taskList, this.getContext().getSelf()));
        }
        return this;
    }
}
