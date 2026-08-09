package org.traccar.game.state.view;

import java.util.Date;
import java.util.List;

public class GameStateView {

    private GameView game;
    private MemberView currentMember;
    private SummaryView summary;
    private OwnRuntimeView ownRuntime;
    private AllowedActionsView allowedActions;
    private List<MemberView> members;
    private SpeedhuntView speedhunt;
    private List<SpeedhuntView> speedhuntHistory;
    private List<JokerView> jokers;
    private List<RevealView> reveals;
    private List<CatchView> catches;
    private List<GeofenceView> geofences;

    public GameView getGame() {
        return game;
    }

    public void setGame(GameView game) {
        this.game = game;
    }

    public MemberView getCurrentMember() {
        return currentMember;
    }

    public void setCurrentMember(MemberView currentMember) {
        this.currentMember = currentMember;
    }

    public SummaryView getSummary() {
        return summary;
    }

    public void setSummary(SummaryView summary) {
        this.summary = summary;
    }

    public OwnRuntimeView getOwnRuntime() {
        return ownRuntime;
    }

    public void setOwnRuntime(OwnRuntimeView ownRuntime) {
        this.ownRuntime = ownRuntime;
    }

    public AllowedActionsView getAllowedActions() {
        return allowedActions;
    }

    public void setAllowedActions(AllowedActionsView allowedActions) {
        this.allowedActions = allowedActions;
    }

    public List<MemberView> getMembers() {
        return members;
    }

    public void setMembers(List<MemberView> members) {
        this.members = members;
    }

    public SpeedhuntView getSpeedhunt() {
        return speedhunt;
    }

    public void setSpeedhunt(SpeedhuntView speedhunt) {
        this.speedhunt = speedhunt;
    }

    public List<SpeedhuntView> getSpeedhuntHistory() {
        return speedhuntHistory;
    }

    public void setSpeedhuntHistory(List<SpeedhuntView> speedhuntHistory) {
        this.speedhuntHistory = speedhuntHistory;
    }

    public List<JokerView> getJokers() {
        return jokers;
    }

    public void setJokers(List<JokerView> jokers) {
        this.jokers = jokers;
    }

    public List<RevealView> getReveals() {
        return reveals;
    }

    public void setReveals(List<RevealView> reveals) {
        this.reveals = reveals;
    }

    public List<CatchView> getCatches() {
        return catches;
    }

    public void setCatches(List<CatchView> catches) {
        this.catches = catches;
    }

    public List<GeofenceView> getGeofences() {
        return geofences;
    }

    public void setGeofences(List<GeofenceView> geofences) {
        this.geofences = geofences;
    }

    public static class GameView {
        private long id;
        private String name;
        private String status;
        private Date startedAt;
        private Date plannedEndAt;
        private Date finishedAt;
        private Long remainingSeconds;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Date getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(Date startedAt) {
            this.startedAt = startedAt;
        }

        public Date getPlannedEndAt() {
            return plannedEndAt;
        }

        public void setPlannedEndAt(Date plannedEndAt) {
            this.plannedEndAt = plannedEndAt;
        }

        public Date getFinishedAt() {
            return finishedAt;
        }

        public void setFinishedAt(Date finishedAt) {
            this.finishedAt = finishedAt;
        }

        public Long getRemainingSeconds() {
            return remainingSeconds;
        }

        public void setRemainingSeconds(Long remainingSeconds) {
            this.remainingSeconds = remainingSeconds;
        }
    }

    public static class MemberView {
        private long id;
        private String displayName;
        private String role;
        private String status;
        private Date caughtAt;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Date getCaughtAt() {
            return caughtAt;
        }

        public void setCaughtAt(Date caughtAt) {
            this.caughtAt = caughtAt;
        }
    }

    public static class SummaryView {
        private Date nextRegularPingAt;
        private Long nextRegularPingInSeconds;
        private int activeHunters;
        private int activeHunted;
        private int caughtHunted;
        private int speedhuntsRemaining;
        private boolean speedhuntActive;
        private int speedhuntPingNumber;
        private int speedhuntPingLimit;

        public Date getNextRegularPingAt() {
            return nextRegularPingAt;
        }

        public void setNextRegularPingAt(Date nextRegularPingAt) {
            this.nextRegularPingAt = nextRegularPingAt;
        }

        public Long getNextRegularPingInSeconds() {
            return nextRegularPingInSeconds;
        }

        public void setNextRegularPingInSeconds(Long nextRegularPingInSeconds) {
            this.nextRegularPingInSeconds = nextRegularPingInSeconds;
        }

        public int getActiveHunters() {
            return activeHunters;
        }

        public void setActiveHunters(int activeHunters) {
            this.activeHunters = activeHunters;
        }

        public int getActiveHunted() {
            return activeHunted;
        }

        public void setActiveHunted(int activeHunted) {
            this.activeHunted = activeHunted;
        }

        public int getCaughtHunted() {
            return caughtHunted;
        }

        public void setCaughtHunted(int caughtHunted) {
            this.caughtHunted = caughtHunted;
        }

        public int getSpeedhuntsRemaining() {
            return speedhuntsRemaining;
        }

        public void setSpeedhuntsRemaining(int speedhuntsRemaining) {
            this.speedhuntsRemaining = speedhuntsRemaining;
        }

        public boolean getSpeedhuntActive() {
            return speedhuntActive;
        }

        public void setSpeedhuntActive(boolean speedhuntActive) {
            this.speedhuntActive = speedhuntActive;
        }

        public int getSpeedhuntPingNumber() {
            return speedhuntPingNumber;
        }

        public void setSpeedhuntPingNumber(int speedhuntPingNumber) {
            this.speedhuntPingNumber = speedhuntPingNumber;
        }

        public int getSpeedhuntPingLimit() {
            return speedhuntPingLimit;
        }

        public void setSpeedhuntPingLimit(int speedhuntPingLimit) {
            this.speedhuntPingLimit = speedhuntPingLimit;
        }
    }

    public static class OwnRuntimeView {
        private boolean activeJokerEffect;
        private List<String> activeJokerTypes;
        private boolean visibleSpeedhuntTarget;

        public boolean getActiveJokerEffect() {
            return activeJokerEffect;
        }

        public void setActiveJokerEffect(boolean activeJokerEffect) {
            this.activeJokerEffect = activeJokerEffect;
        }

        public List<String> getActiveJokerTypes() {
            return activeJokerTypes;
        }

        public void setActiveJokerTypes(List<String> activeJokerTypes) {
            this.activeJokerTypes = activeJokerTypes;
        }

        public boolean getVisibleSpeedhuntTarget() {
            return visibleSpeedhuntTarget;
        }

        public void setVisibleSpeedhuntTarget(boolean visibleSpeedhuntTarget) {
            this.visibleSpeedhuntTarget = visibleSpeedhuntTarget;
        }
    }

    public static class AllowedActionsView {
        private boolean canStartSpeedhunt;
        private boolean canRequestSpeedhuntPing;
        private boolean canUseJoker;
        private boolean canManageRuntime;

        public boolean getCanStartSpeedhunt() {
            return canStartSpeedhunt;
        }

        public void setCanStartSpeedhunt(boolean canStartSpeedhunt) {
            this.canStartSpeedhunt = canStartSpeedhunt;
        }

        public boolean getCanRequestSpeedhuntPing() {
            return canRequestSpeedhuntPing;
        }

        public void setCanRequestSpeedhuntPing(boolean canRequestSpeedhuntPing) {
            this.canRequestSpeedhuntPing = canRequestSpeedhuntPing;
        }

        public boolean getCanUseJoker() {
            return canUseJoker;
        }

        public void setCanUseJoker(boolean canUseJoker) {
            this.canUseJoker = canUseJoker;
        }

        public boolean getCanManageRuntime() {
            return canManageRuntime;
        }

        public void setCanManageRuntime(boolean canManageRuntime) {
            this.canManageRuntime = canManageRuntime;
        }
    }

    public static class SpeedhuntView {
        private boolean active;
        private long id;
        private int sequenceNumber;
        private Long targetMemberId;
        private String targetDisplayName;
        private boolean targetRevealed;
        private int pingNumber;
        private int maxPings;
        private Date startedAt;
        private Date endedAt;

        public boolean getActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public int getSequenceNumber() {
            return sequenceNumber;
        }

        public void setSequenceNumber(int sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }

        public Long getTargetMemberId() {
            return targetMemberId;
        }

        public void setTargetMemberId(Long targetMemberId) {
            this.targetMemberId = targetMemberId;
        }

        public String getTargetDisplayName() {
            return targetDisplayName;
        }

        public void setTargetDisplayName(String targetDisplayName) {
            this.targetDisplayName = targetDisplayName;
        }

        public boolean getTargetRevealed() {
            return targetRevealed;
        }

        public void setTargetRevealed(boolean targetRevealed) {
            this.targetRevealed = targetRevealed;
        }

        public int getPingNumber() {
            return pingNumber;
        }

        public void setPingNumber(int pingNumber) {
            this.pingNumber = pingNumber;
        }

        public int getMaxPings() {
            return maxPings;
        }

        public void setMaxPings(int maxPings) {
            this.maxPings = maxPings;
        }

        public Date getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(Date startedAt) {
            this.startedAt = startedAt;
        }

        public Date getEndedAt() {
            return endedAt;
        }

        public void setEndedAt(Date endedAt) {
            this.endedAt = endedAt;
        }
    }

    public static class JokerView {
        private long id;
        private Long memberId;
        private String memberDisplayName;
        private String type;
        private String status;
        private Date unlockedAt;
        private Date activatedAt;
        private Date usedAt;
        private Date cancelledAt;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public Long getMemberId() {
            return memberId;
        }

        public void setMemberId(Long memberId) {
            this.memberId = memberId;
        }

        public String getMemberDisplayName() {
            return memberDisplayName;
        }

        public void setMemberDisplayName(String memberDisplayName) {
            this.memberDisplayName = memberDisplayName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Date getUnlockedAt() {
            return unlockedAt;
        }

        public void setUnlockedAt(Date unlockedAt) {
            this.unlockedAt = unlockedAt;
        }

        public Date getActivatedAt() {
            return activatedAt;
        }

        public void setActivatedAt(Date activatedAt) {
            this.activatedAt = activatedAt;
        }

        public Date getUsedAt() {
            return usedAt;
        }

        public void setUsedAt(Date usedAt) {
            this.usedAt = usedAt;
        }

        public Date getCancelledAt() {
            return cancelledAt;
        }

        public void setCancelledAt(Date cancelledAt) {
            this.cancelledAt = cancelledAt;
        }
    }

    public static class RevealView {
        private long id;
        private String type;
        private long speedhuntId;
        private String payload;
        private Date revealedAt;
        private Date invalidatedAt;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getSpeedhuntId() {
            return speedhuntId;
        }

        public void setSpeedhuntId(long speedhuntId) {
            this.speedhuntId = speedhuntId;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public Date getRevealedAt() {
            return revealedAt;
        }

        public void setRevealedAt(Date revealedAt) {
            this.revealedAt = revealedAt;
        }

        public Date getInvalidatedAt() {
            return invalidatedAt;
        }

        public void setInvalidatedAt(Date invalidatedAt) {
            this.invalidatedAt = invalidatedAt;
        }
    }

    public static class CatchView {
        private long id;
        private long caughtMemberId;
        private String caughtDisplayName;
        private String status;
        private Date caughtAt;
        private Date revertedAt;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getCaughtMemberId() {
            return caughtMemberId;
        }

        public void setCaughtMemberId(long caughtMemberId) {
            this.caughtMemberId = caughtMemberId;
        }

        public String getCaughtDisplayName() {
            return caughtDisplayName;
        }

        public void setCaughtDisplayName(String caughtDisplayName) {
            this.caughtDisplayName = caughtDisplayName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Date getCaughtAt() {
            return caughtAt;
        }

        public void setCaughtAt(Date caughtAt) {
            this.caughtAt = caughtAt;
        }

        public Date getRevertedAt() {
            return revertedAt;
        }

        public void setRevertedAt(Date revertedAt) {
            this.revertedAt = revertedAt;
        }
    }

    public static class GeofenceView {
        private long id;
        private long geofenceId;
        private String name;
        private String type;
        private String role;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getGeofenceId() {
            return geofenceId;
        }

        public void setGeofenceId(long geofenceId) {
            this.geofenceId = geofenceId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

}
