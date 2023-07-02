package com.example;

import akka.actor.typed.ActorRef;
import java.util.UUID;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.japi.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;


public class Scheduler extends AbstractBehavior<Scheduler.Message> {

    public interface Message {
    }

    ;


    public static class CreateTask implements Message {
    }

    public static class TaskIsCreated implements Message {
        String taskName;
        ActorRef<Tasks.Message> task;
        int neededNumberOfWorkers;

        public TaskIsCreated(String taskName, ActorRef<Tasks.Message> task, int neededNumberOfWorkers) {
            this.taskName = taskName;
            this.task = task;
            this.neededNumberOfWorkers = neededNumberOfWorkers;
        }
    }

    public static class TaskIsDone implements Message { }

    public static class WorkerIsDone implements Message { }

    public static Behavior<Message> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new Scheduler(context)));
    }

    private static final int MAX_WORKERS = 20;
    private int activeWorkers;
    private Queue<Pair<ActorRef<Tasks.Message>, Integer>> taskQueue;

    private Scheduler(ActorContext<Message> context) {
        super(context);
        taskQueue = new LinkedList<>();
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(CreateTask.class, this::onCreateTask)
                .onMessage(TaskIsCreated.class, this::isCreated)
                .onMessage(TaskIsDone.class, this::onTaskIsDone)
                .onMessage(WorkerIsDone.class, this::onWorkerIsDone)
                .build();
    }

    private Behavior<Message> onCreateTask(CreateTask msg) throws InterruptedException {
        int countTasks = 1;
        while (countTasks <= 20) {
            getContext().getLog().info("I AM HERE");
            this.getContext().spawn(Tasks.create(countTasks, this.getContext().getSelf()), "task" + countTasks);
            countTasks++;
        }
        return this;
    }

    private Behavior<Message> isCreated(TaskIsCreated msg) {
        if (msg.neededNumberOfWorkers < MAX_WORKERS - activeWorkers) {
            createWorkersForTask(msg.neededNumberOfWorkers, msg.task);
        } else {
            getContext().getLog().info("Added task into queue" + msg.taskName);
            this.taskQueue.add(new Pair<>(msg.task, msg.neededNumberOfWorkers));
        }
        return this;
    }

    private void createWorkersForTask(int neededNumberOfWorkers, ActorRef<Tasks.Message> task) {
        activeWorkers += neededNumberOfWorkers;
        ArrayList<ActorRef<Worker.Message>> neededWorkers = new ArrayList<>();
        for (int i = 0; i < neededNumberOfWorkers; i++) {
            UUID uniqueId = UUID.randomUUID();
            ActorRef<Worker.Message> newWorker = this.getContext().spawn(Worker.create(this.getContext().getSelf(), task), "worker" + uniqueId);
            neededWorkers.add(newWorker);
        }
        task.tell(new Tasks.CreatedWorkers(neededWorkers));
    }

    private Behavior<Message> onTaskIsDone(TaskIsDone msg) {
        if (!taskQueue.isEmpty()){
            Pair<ActorRef<Tasks.Message>, Integer> currentTask = taskQueue.poll();
            if (currentTask.second() < MAX_WORKERS - activeWorkers){
                createWorkersForTask(currentTask.second(), currentTask.first());
            }
        }
        else{
            getContext().getLog().info("All tasks are done!");
        }
        return this;
    }

    private Behavior<Message> onWorkerIsDone(WorkerIsDone msg) {
        activeWorkers -= 1;
        return this;
    }
}
