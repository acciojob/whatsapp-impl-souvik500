package com.driver;

import java.util.*;

import io.swagger.models.auth.In;
import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below-mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }

    public String createUser(String name, String mobile) throws Exception {
        if(userMobile.contains(mobile)){
            throw new RuntimeException("User already exists");
        }else{
            User user = new User(name,mobile);
            userMobile.add(mobile);
        }
        return "SUCCESS";
    }

    public Group createGroup(List<User> users) {

        //A user can belong to exactly one group and has a unique name. how to handle if not unique and also present in another group?
        if(users.size() == 2){
            Group group = new Group(users.get(1).getName(),users.size());
            groupUserMap.put(group,users);
            groupMessageMap.put(group,new ArrayList<Message>());
            return group;
        }else {
            customGroupCount++;
            Group group = new Group("Group " + customGroupCount,users.size());
            adminMap.put(group,users.get(0));
            groupMessageMap.put(group,new ArrayList<Message>());
            groupUserMap.put(group,users);
            return group;
        }
    }

    public int createMessage(String content) {
        messageId++;
        Message message = new Message(messageId,content);
        return message.getId();
    }

    public int sendMessage(Message message, User sender, Group group) throws RuntimeException {
        if(!groupUserMap.containsKey(group)){
            throw new RuntimeException("Group does not exist");
        }else{
            for(User user : groupUserMap.get(group)){
                if(user.equals(sender)){
                    List<Message> messageList= groupMessageMap.get(group);
                    messageList.add(message);
                    senderMap.put(message,sender);
                    return  messageList.size();
                }
            }
            throw new RuntimeException("You are not allowed to send message");
        }
    }

    public String changeAdmin(User approver, User user, Group group)throws RuntimeException {
        if(!groupUserMap.containsKey(group)){
            throw new RuntimeException("Group does not exist");
        } else if (!adminMap.containsKey(group) || !adminMap.get(group).equals(approver)) {
            throw new RuntimeException("Approver does not have rights");
        }else {
            adminMap.put(group,user);
            return "SUCCESS";
        }
    }

    public int removeUser(User user)throws Exception {
        // get Group in list from groupUSer map, get user index list
        // get message list from sender index with help of user
        // get List of message index from group message map

        int res =0;
        for (Group group : adminMap.keySet()){
            if(adminMap.get(group).equals(user))throw  new RuntimeException("User is admin cannot delete");
        }
        Map<Group,User> groupList = new HashMap<>(); // only the list of group where user exist;
        for(Group group : groupUserMap.keySet()){
            for (User user1 : groupUserMap.get(group)){
                if(user1.equals(user)){
                    groupList.put(group,user);
                }
            }
        }
        if(groupList.size() == 0) throw new RuntimeException("user is not found");

        for(Group group : groupList.keySet()){
            User user1 = groupList.get(group);
            groupUserMap.get(group).remove(user1);
            res += groupUserMap.get(group).size();
        }

        List<Message> messageList = new ArrayList<>();
        for(Message message : senderMap.keySet()){
            if(senderMap.get(message).equals(user)){
                messageList.add(message);
            }
        }

        Map<Group,List<Message>> messageListMap = new HashMap<>();
        for (Message original : messageList){
            for(Group group : groupList.keySet()){
                List<Message> messageList1 =groupMessageMap.get(group);
                for(Message check : messageList1 ){
                    if(check.equals(original)){
                        List<Message> ls = messageListMap.getOrDefault(group, new ArrayList<Message>());
                        ls.add(check);
                        messageListMap.put(group,ls);
                    }
                }
            }
        }
        for (Group group : messageListMap.keySet()){
            List<Message> remove = messageListMap.get(group);
            for (Message message : remove){
                groupMessageMap.get(group).remove(message);
                res += groupMessageMap.get(group).size();
                senderMap.remove(message);
            }
        }

        for (Group group : groupMessageMap.keySet()){
            res += groupMessageMap.get(group).size();
        }
        return res;
    }

    public String findMessage(Date start, Date end, int k) {
        PriorityQueue<Pair> priorityQueue = new PriorityQueue<>((Pair a, Pair b) ->(b.size- a.size));

        int messageCount =0;
        for (Message message : senderMap.keySet()){
            if (message.getTimestamp().after(start) && message.getTimestamp().before(end)){
                messageCount++;
                priorityQueue.add(new Pair(message.getContent(),message.getContent().length()));
            }
        }
        if (priorityQueue.size() < messageCount) throw  new RuntimeException("number of messages between the given time is less than K");
        return priorityQueue.peek().string;

    }
}