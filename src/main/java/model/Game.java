package model;

/*-
 * #%L
 * Zork Clone
 * %%
 * Copyright (C) 2016 Frederik Kammel
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.github.vatbub.common.core.Common;
import com.github.vatbub.common.core.logging.FOKLogger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import org.jetbrains.annotations.NotNull;
import view.GameMessage;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * Represents the game as a whole, all the rooms, items and the current {@link Player}.
 */
@SuppressWarnings("unused")
public class Game implements Serializable {

    /**
     * The app version that was used when the game was saved.
     */
    @SuppressWarnings({"unused"})
    public final String gameSavedWithAppVersion = Common.getAppVersion();

    private Room currentRoom;
    private Player player;
    private int score;
    private int moveCount;
    private List<GameMessage> messages;

    /**
     * If the game was loaded from a file, this specifies the file it was loaded from. {@code null} if the game was not loaded from a file.
     */
    private transient File fileSource;

    /**
     * {@code true} if this game was modified since the last save, {@code false} otherwise
     */
    private transient BooleanProperty modified;

    private transient ChangeListener<Boolean> roomModificationListener;

    private transient RoomMap.ChangeListener roomMapModificationListener;

    public Game() {
        this(new Room());
    }

    public Game(Room currentRoom) {
        this(currentRoom, new Player());
    }

    public Game(Room currentRoom, Player player) {
        this(currentRoom, player, 0);
    }

    public Game(Room currentRoom, Player player, @SuppressWarnings("SameParameterValue") int score) {
        this(currentRoom, player, score, 0);
    }

    public Game(Room currentRoom, Player player, int score, @SuppressWarnings("SameParameterValue") int moveCount) {
        this(currentRoom, player, score, moveCount, new ArrayList<>());
    }

    public Game(Room currentRoom, Player player, int score, int moveCount, List<GameMessage> messages) {
        initListeners();

        modifiedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                // set modified for all rooms
                LinkedList<Room> roomQueue = new LinkedList<>();
                roomQueue.add(this.getCurrentRoom());
                while (!roomQueue.isEmpty()) {
                    Room room = roomQueue.remove();
                    System.out.println(room.getName());
                    room.setModified(false);
                    for (Map.Entry<WalkDirection, Room> entry : room.getAdjacentRooms().entrySet()) {
                        if (entry.getValue().isModified()) {
                            roomQueue.add(entry.getValue());
                        }
                    }
                }
            }
        });

        this.setMessages(messages);
        this.setMoveCount(moveCount);
        this.setScore(score);
        this.setPlayer(player);
        this.setCurrentRoom(currentRoom);
    }

    /**
     * Loads a game from the specified {@code File}.
     *
     * @param saveFileLocation The fully qualified path and name of the file to load the save from.
     * @return The game that was saved in that file
     * @throws IOException            If the specified file does not exist or cannot be read for some other reason.
     * @throws ClassNotFoundException If the specified file does not contain a {@code Game} but anything else (wrong file format)
     * @see #save()
     * @see #save(String)
     * @see #save(File)
     * @see #gameSavedWithAppVersion
     */
    public static Game load(String saveFileLocation) throws IOException, ClassNotFoundException {
        return Game.load(new File(saveFileLocation));
    }

    /**
     * Loads a game from the specified {@code File}.
     *
     * @param saveFile The file to load the save from
     * @return The game that was saved in that file
     * @throws IOException            If the specified file does not exist or cannot be read for some other reason.
     * @throws ClassNotFoundException If the specified file does not contain a {@code Game} but anything else (wrong file format)
     */
    public static Game load(@NotNull File saveFile) throws IOException, ClassNotFoundException {
        Objects.requireNonNull(saveFile);

        FileInputStream fileIn = new FileInputStream(saveFile);
        ObjectInputStream objIn = new ObjectInputStream(fileIn);

        Game res = (Game) objIn.readObject();
        res.setFileSource(saveFile);
        res.getCurrentRoom().setIsCurrentRoom(true);
        return res;
    }

    private void initListeners() {
        if (roomModificationListener == null) {
            roomModificationListener = ((observable, oldValue, newValue) -> {
                if (newValue) {
                    System.out.println("Room was modified");
                    setModified(true);
                }
            });
        }

        if (roomMapModificationListener == null) {
            roomMapModificationListener = new RoomMap.ChangeListener() {
                @Override
                public void removed(WalkDirection key, Room value) {
                    FOKLogger.finest(Game.class.getName(), "Adjacent room was removed");
                    if (value != null) {
                        value.modifiedProperty().removeListener(roomModificationListener);
                        if (value.getAdjacentRooms().getChangeListenerList().contains(roomMapModificationListener)) {
                            value.getAdjacentRooms().getChangeListenerList().remove(roomMapModificationListener);
                        }
                    }
                }

                @Override
                public void added(WalkDirection key, Room value) {
                    FOKLogger.finest(Game.class.getName(), "Adjacent room was added");
                    value.modifiedProperty().removeListener(roomModificationListener);
                    value.modifiedProperty().addListener(roomModificationListener);
                    if (!value.getAdjacentRooms().getChangeListenerList().contains(roomMapModificationListener)) {
                        value.getAdjacentRooms().getChangeListenerList().add(roomMapModificationListener);
                    }
                }

                @Override
                public void replaced(WalkDirection key, Room oldValue, Room newValue) {
                    FOKLogger.finest(Game.class.getName(), "Adjacent room was replaced");
                    newValue.modifiedProperty().removeListener(roomModificationListener);
                    newValue.modifiedProperty().addListener(roomModificationListener);
                    if (!newValue.getAdjacentRooms().getChangeListenerList().contains(roomMapModificationListener)) {
                        newValue.getAdjacentRooms().getChangeListenerList().add(roomMapModificationListener);
                    }
                }
            };
        }
    }

    public int getMoveCount() {
        return moveCount;
    }

    public void setMoveCount(int moveCount) {
        this.moveCount = moveCount;
        setModified(true);
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
        setModified(true);
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
        setModified(true);
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room currentRoom) {
        initListeners();

        if (this.currentRoom != null) {
            this.currentRoom.setIsCurrentRoom(false);
        }
        this.currentRoom = currentRoom;
        this.currentRoom.setIsCurrentRoom(true);
        this.currentRoom.modifiedProperty().addListener(roomModificationListener);
        //noinspection SuspiciousMethodCalls
        if (!this.currentRoom.getAdjacentRooms().getChangeListenerList().contains(roomModificationListener)) {
            this.currentRoom.getAdjacentRooms().getChangeListenerList().add(roomMapModificationListener);
        }
        setModified(true);
    }

    public List<GameMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<GameMessage> messages) {
        this.messages = messages;
        setModified(true);
    }

    public String getGameSavedWithAppVersion() {
        return gameSavedWithAppVersion;
    }

    /**
     * The {@code File} the game was loaded from or {@code null} if it was not loaded from a file.
     *
     * @return The {@code File} the game was loaded from or {@code null} if it was not loaded from a file.
     */
    public File getFileSource() {
        return fileSource;
    }

    public void setFileSource(File fileSource) {
        this.fileSource = fileSource;
    }

    /**
     * Checks if this game was modified since the last save.
     *
     * @return {@code true} if this game was modified since the last save, {@code false} otherwise
     */
    public boolean isModified() {
        if (this.modified == null) {
            modified = new SimpleBooleanProperty();
        }

        return modified.getValue();
    }

    private void setModified(boolean modified) {
        if (this.modified == null) {
            this.modified = new SimpleBooleanProperty();
        }

        this.modified.set(modified);
    }

    public BooleanProperty modifiedProperty() {
        if (this.modified == null) {
            modified = new SimpleBooleanProperty();
        }

        return modified;
    }

    public void save() {
        this.save("");
    }

    /**
     * Saves this game state on the hard disk so that it can be loaded later on using {@link #load(String)} or {@link #load(File)}.<br>
     * The file name is generated automatically using the following scheme:<br>
     * <ul>
     * <li>If {@code customNamePrefix} equals {@code ""} (same as calling {@link #save()}:<br>
     * {@code %appdata%/zork/saves/yyyy-MM-dd-HH-mm-ss.fokGameSave}</li>
     * <li>If {@code customNamePrefix} does not equal {@code ""}:<br>
     * {@code %appdata%/zork/saves/customNamePrefix-yyyy-MM-dd-HH-mm-ss.fokGameSave}</li>
     * </ul>
     * where {@code yyyy-MM-dd-HH-mm-ss} is the current time and day.
     *
     * @param customNamePrefix The prefix of the file name
     * @see #load(File)
     * @see #load(String)
     */
    public void save(@SuppressWarnings("SameParameterValue") @NotNull String customNamePrefix) {
        Objects.requireNonNull(customNamePrefix);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date();
        String dateString = dateFormat.format(date);

        String fileName = Common.getAndCreateAppDataPath() + "saves" + File.separator;

        if (!customNamePrefix.equals("")) {
            // We have a name given
            fileName = fileName + customNamePrefix + "-" + dateString;
        } else {
            fileName = fileName + dateString;
        }

        fileName = fileName + ".fokGameSave";

        try {
            this.save(new File(fileName));
        } catch (IOException e) {
            FOKLogger.log(Game.class.getName(), Level.SEVERE, "Something magical happened and the user tried to save two games at exactly the same time. Here's the result:", e);
        }
    }

    /**
     * Saves this game state to the specified file so that it can be loaded later on using {@link #load(String)} or {@link #load(File)}.
     *
     * @param fileToSave The file to save the game in. If the file exists already, it is overwritten.
     * @throws IOException If the specified file cannot be written for any reason.
     * @see #load(File)
     * @see #load(String)
     */
    public void save(@NotNull File fileToSave) throws IOException {
        Objects.requireNonNull(fileToSave);

        if (fileToSave.exists()) {
            // delete the file
            //noinspection ResultOfMethodCallIgnored
            fileToSave.delete();
        }

        // Serialize this object at the given file
        FileOutputStream fileOut = new FileOutputStream(fileToSave);
        ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
        objOut.writeObject(this);
        objOut.close();
        fileOut.close();

        this.setFileSource(fileToSave);
        this.setModified(false);
    }
}
