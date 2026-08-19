package org.traccar.game.state.view;

import java.util.Date;
import java.util.List;

public class GameStateView {

    private GameView game;
    private MemberView currentMember;
    private SummaryView summary;
    private AllowedActionsView allowedActions;
    private List<MemberView> members;
    private List<GeofenceView> geofences;
    private List<SpeedhuntView> speedhuntHistory;
    private List<JokerView> jokers;

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

    public List<GeofenceView> getGeofences() {
        return geofences;
    }

    public void setGeofences(List<GeofenceView> geofences) {
        this.geofences = geofences;
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
        private int speedhuntsRemaining;
        private boolean speedhuntActive;
        private Long speedhuntId;
        private int speedhuntPingNumber;
        private int speedhuntPingLimit;
        private boolean speedhuntTargetRevealed;
        private Long speedhuntTargetMemberId;
        private String speedhuntTargetDisplayName;
        private boolean activeJokerEffect;
        private List<String> activeJokerTypes = List.of();

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

        public Long getSpeedhuntId() {
            return speedhuntId;
        }

        public void setSpeedhuntId(Long speedhuntId) {
            this.speedhuntId = speedhuntId;
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

        public boolean getSpeedhuntTargetRevealed() {
            return speedhuntTargetRevealed;
        }

        public void setSpeedhuntTargetRevealed(boolean speedhuntTargetRevealed) {
            this.speedhuntTargetRevealed = speedhuntTargetRevealed;
        }

        public Long getSpeedhuntTargetMemberId() {
            return speedhuntTargetMemberId;
        }

        public void setSpeedhuntTargetMemberId(Long speedhuntTargetMemberId) {
            this.speedhuntTargetMemberId = speedhuntTargetMemberId;
        }

        public String getSpeedhuntTargetDisplayName() {
            return speedhuntTargetDisplayName;
        }

        public void setSpeedhuntTargetDisplayName(String speedhuntTargetDisplayName) {
            this.speedhuntTargetDisplayName = speedhuntTargetDisplayName;
        }

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
    }

    public static class GeofenceView {
        private long id;
        private String name;
        private String type;
        private String role;
        private boolean active;

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

        public boolean getActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    public static class AllowedActionsView {
        private boolean canStartSpeedhunt;
        private boolean canRequestSpeedhuntPing;
        private boolean canUseJoker;
        private boolean canManageRuntime;
        private boolean canManageGeofences;
        private boolean canUnlockJoker;

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

        public boolean getCanManageGeofences() {
            return canManageGeofences;
        }

        public void setCanManageGeofences(boolean canManageGeofences) {
            this.canManageGeofences = canManageGeofences;
        }

        public boolean getCanUnlockJoker() {
            return canUnlockJoker;
        }

        public void setCanUnlockJoker(boolean canUnlockJoker) {
            this.canUnlockJoker = canUnlockJoker;
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
        private List<SpeedhuntPingView> pings = List.of();

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

        public List<SpeedhuntPingView> getPings() {
            return pings;
        }

        public void setPings(List<SpeedhuntPingView> pings) {
            this.pings = pings;
        }
    }

    public static class SpeedhuntPingView {
        private long id;
        private int sequenceNumber;
        private Date createdAt;
        private Date fixTime;
        private boolean skipped;

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

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }

        public Date getFixTime() {
            return fixTime;
        }

        public void setFixTime(Date fixTime) {
            this.fixTime = fixTime;
        }

        public boolean getSkipped() {
            return skipped;
        }

        public void setSkipped(boolean skipped) {
            this.skipped = skipped;
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

}
