// Alla Spitzer 222114
// Olha Borysova 230606
// Anastasiia Kulyani 230612
// Dmytro Pahuba 230665

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

    //erstellt einen Task
    public static class CreateTask implements Message {
    }

    //fordert neue Workers an
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
                .onMessage(WorkerIsDone.class, this::onWorkerIsDone)
                .build();
    }

    //erstellt 20 Tasks
    private Behavior<Message> onCreateTask(CreateTask msg) throws InterruptedException {
        int countTasks = 1;
        while (countTasks <= 20) {
            this.getContext().spawn(Tasks.create(countTasks, this.getContext().getSelf()), "task" + countTasks);
            countTasks++;
        }
        return this;
    }

    //wenn maximale Anzahl an Workers nicht überschritten wurde, erstellt neue Workers für ein Task
    private Behavior<Message> isCreated(TaskIsCreated msg) {
        if (msg.neededNumberOfWorkers < MAX_WORKERS - activeWorkers) {
            createWorkersForTask(msg.neededNumberOfWorkers, msg.task);
        } else {
            getContext().getLog().info("Added task into queue" + msg.taskName);
            this.taskQueue.add(new Pair<>(msg.task, msg.neededNumberOfWorkers));
        }
        return this;
    }

    //initialisiert und übergibt erstellte Workers an Task
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

    //prüft den nächsten auszuführenden Task und passt die Anzahl an aktiven Workern entsprechend an
    private Behavior<Message> onWorkerIsDone(WorkerIsDone msg) {
        activeWorkers -= 1;
        if (!taskQueue.isEmpty()){
            Pair<ActorRef<Tasks.Message>, Integer> currentTask = taskQueue.peek();
            if (currentTask.second() <= MAX_WORKERS - activeWorkers){
                currentTask = taskQueue.poll();
                createWorkersForTask(currentTask.second(), currentTask.first());
            }
        }
        else if(activeWorkers == 0){
            getContext().getLog().info("All tasks are done!");
        }
        return this;
    }
}
