package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private HashMap<String,User> userDB;
    private HashMap<Integer,Message> messageDB;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.userDB = new HashMap<String,User>();
        this.messageDB = new HashMap<Integer,Message>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }

    public String createUser(String name, String mobile) throws Exception{
        User user = new User(name, mobile);
        if(userMobile.contains(mobile)){
            throw new Exception("User already exists");
        }
        else{
            userMobile.add(mobile);
            userDB.put(name,user);
            return "SUCCESS";
        }
    }

    public Group createGroup(List<User> users){
        // The list contains at least 2 users where the first user is the admin. A group has exactly one admin.
        // If there are only 2 users, the group is a personal chat and the group name should be kept as the name of the second user(other than admin)
        // If there are 2+ users, the name of group should be "Group count". For example, the name of first group would be "Group 1", second would be "Group 2" and so on.
        // Note that a personal chat is not considered a group and the count is not updated for personal chats.
        // If group is successfully created, return group.

        //For example: Consider userList1 = {Alex, Bob, Charlie}, userList2 = {Dan, Evan}, userList3 = {Felix, Graham, Hugh}.
        //If createGroup is called for these userLists in the same order, their group names would be "Group 1", "Evan", and "Group 2" respectively.

        Group  group = new Group(" ",users.size());
        String groupName = "";
        if(users.size()==2){
            groupName = users.get(1).getName();
        }
        else if(users.size()>2){
            customGroupCount++;
            groupName = String.format("Group %d", customGroupCount);
        }
        group.setName(groupName);
        groupUserMap.put(group, users);
        adminMap.put(group,users.get(0));
        return group;
    }

    public int createMessage(String content){
        // The 'i^th' created message has message id 'i'.
        // Return the message id.
        messageId++;
        Message message = new Message(messageId, content);
        messageDB.put(messageId,message);
        return messageId;
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "You are not allowed to send message" if the sender is not a member of the group
        //If the message is sent successfully, return the final number of messages in that group.
        int numberOfMessages = 0;

        if(groupUserMap.containsKey(group)){
            if(groupUserMap.get(group).contains(sender)){
                List<Message> messages = new ArrayList<>();
                if(groupMessageMap.containsKey(group))
                    messages = groupMessageMap.get(group);
                messages.add(message);
                groupMessageMap.put(group,messages);
                senderMap.put(message,sender);
                numberOfMessages = messages.size();
            }
            else{
                throw new Exception("You are not allowed to send message");
            }
        }
        else{
            throw new Exception("Group does not exist");
        }
        return numberOfMessages;
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "Approver does not have rights" if the approver is not the current admin of the group
        //Throw "User is not a participant" if the user is not a part of the group
        //Change the admin of the group to "user" and return "SUCCESS". Note that at one time there is only one admin and the admin rights are transferred from approver to user.
        if(groupUserMap.containsKey(group)){
            if(adminMap.get(group).equals(approver)){
                if(groupUserMap.get(group).contains(user)){
                    adminMap.put(group, user);
                }
                else{
                    throw new Exception("User is not a participant");
                }
            }
            else{
                throw new Exception("Approver does not have rights");
            }
        }
        else{
            throw new Exception("Group does not exist");
        }
        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception{
        //This is a bonus problem and does not contains any marks
        //A user belongs to exactly one group
        //If user is not found in any group, throw "User not found" exception
        //If user is found in a group and it is the admin, throw "Cannot remove admin" exception
        //If user is not the admin, remove the user from the group, remove all its messages from all the databases, and update relevant attributes accordingly.
        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)
        int ans = 0;
        boolean isPresent = false;
        Group groupForUser = null;

        for(Group group: groupUserMap.keySet()){
            List<User> users = groupUserMap.get(group);
            if(users.contains(user)){
                if(adminMap.get(group).equals(user)){
                    throw new Exception("Cannot remove admin");
                }
                isPresent = true;
                groupForUser = group;
                break;
            }
        }

        if(isPresent==false)
            throw new Exception("User not found");
        else{
            List<User> updatedUser = new ArrayList<>();
            for(User groupUser:groupUserMap.get(groupForUser)){
                if(groupUser.equals(user)){
                    continue;
                }
                updatedUser.add(groupUser);
            }
            groupUserMap.put(groupForUser, updatedUser);

            List<Message> updatedMessage = new ArrayList<>();
            for(Message message:groupMessageMap.get(groupForUser)){
                if(senderMap.get(message).equals(user))
                    continue;
                updatedMessage.add(message);
            }
            groupMessageMap.put(groupForUser, updatedMessage);

            HashMap<Message, User> updatedSenderMap = new HashMap<>();
            for(Message message: senderMap.keySet()){
                if(senderMap.get(message).equals(user))
                    continue;
                updatedSenderMap.put(message, senderMap.get(message));
            }
            senderMap = updatedSenderMap;
            ans =  updatedUser.size()+updatedMessage.size()+updatedSenderMap.size();
        }
        return ans;
    }

    public String findMessage(Date start, Date end, int K) throws Exception{
        //This is a bonus problem and does not contains any marks
        // Find the Kth latest message between start and end (excluding start and end)
        // If the number of messages between given time is less than K, throw "K is greater than the number of messages" exception
        List<Message>messages = new ArrayList<>();
        for(Message message:senderMap.keySet()){
            Date curr = message.getTimestamp();
            if(start.compareTo(curr)>0 && end.compareTo(curr)<0){
                messages.add(message);
            }
        }

        if(messages.size()<K){
            throw new Exception("K is greater than the number of messages");
        }
        else{
            Collections.sort(messages,new Comparator<Message>(){
                @Override
                public int compare(Message o1, Message o2) {
                    return o1.getTimestamp().compareTo(o2.getTimestamp());
                }
            });
        }
        return messages.get(K-1).getContent();
    }
}
