package org.whispersystems.signalservice.api.groupsv2;

import com.google.protobuf.ByteString;

import org.signal.libsignal.protocol.logging.Log;
import org.signal.storageservice.protos.groups.AccessControl;
import org.signal.storageservice.protos.groups.Member;
import org.signal.storageservice.protos.groups.local.DecryptedApproveMember;
import org.signal.storageservice.protos.groups.local.DecryptedBannedMember;
import org.signal.storageservice.protos.groups.local.DecryptedGroup;
import org.signal.storageservice.protos.groups.local.DecryptedGroupChange;
import org.signal.storageservice.protos.groups.local.DecryptedMember;
import org.signal.storageservice.protos.groups.local.DecryptedModifyMemberRole;
import org.signal.storageservice.protos.groups.local.DecryptedPendingMember;
import org.signal.storageservice.protos.groups.local.DecryptedPendingMemberRemoval;
import org.signal.storageservice.protos.groups.local.DecryptedRequestingMember;
import org.signal.storageservice.protos.groups.local.EnabledState;
import org.whispersystems.signalservice.api.push.ServiceId;
import org.whispersystems.signalservice.api.push.ServiceId.ACI;
import org.whispersystems.signalservice.api.push.ServiceIds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class DecryptedGroupUtil {

  private static final String TAG = DecryptedGroupUtil.class.getSimpleName();

  public static ArrayList<ACI> toAciListWithUnknowns(Collection<DecryptedMember> membersList) {
    ArrayList<ACI> serviceIdList = new ArrayList<>(membersList.size());

    for (DecryptedMember member : membersList) {
      serviceIdList.add(ACI.parseOrUnknown(member.getAciBytes()));
    }

    return serviceIdList;
  }

  /** Converts the list of members to ACI's, filtering out unknown ACI's. */
  public static ArrayList<ACI> toAciList(Collection<DecryptedMember> membersList) {
    ArrayList<ACI> serviceIdList = new ArrayList<>(membersList.size());

    for (DecryptedMember member : membersList) {
      ACI aci = ACI.parseOrNull(member.getAciBytes());

      if (aci != null) {
        serviceIdList.add(aci);
      }
    }

    return serviceIdList;
  }

  public static Set<ByteString> membersToAciByteStringSet(Collection<DecryptedMember> membersList) {
    Set<ByteString> aciList = new HashSet<>(membersList.size());

    for (DecryptedMember member : membersList) {
      aciList.add(member.getAciBytes());
    }

    return aciList;
  }

  /**
   * Can return non-decryptable member ACIs as unknown ACIs.
   */
  public static ArrayList<ServiceId> pendingToServiceIdList(Collection<DecryptedPendingMember> membersList) {
    ArrayList<ServiceId> serviceIdList = new ArrayList<>(membersList.size());

    for (DecryptedPendingMember member : membersList) {
      ServiceId serviceId = ServiceId.parseOrNull(member.getServiceIdBytes());
      if (serviceId != null) {
        serviceIdList.add(serviceId);
      } else {
        serviceIdList.add(ACI.UNKNOWN);
      }
    }

    return serviceIdList;
  }

  /**
   * Will not return any non-decryptable member ACIs.
   */
  public static ArrayList<ServiceId> removedMembersServiceIdList(DecryptedGroupChange groupChange) {
    List<ByteString>     deletedMembers = groupChange.getDeleteMembersList();
    ArrayList<ServiceId> serviceIdList  = new ArrayList<>(deletedMembers.size());

    for (ByteString member : deletedMembers) {
      ServiceId serviceId = ServiceId.parseOrNull(member);

      if (serviceId != null) {
        serviceIdList.add(serviceId);
      }
    }

    return serviceIdList;
  }

  /**
   * Will not return any non-decryptable member ACIs.
   */
  public static ArrayList<ServiceId> removedPendingMembersServiceIdList(DecryptedGroupChange groupChange) {
    List<DecryptedPendingMemberRemoval> deletedPendingMembers = groupChange.getDeletePendingMembersList();
    ArrayList<ServiceId>                serviceIdList         = new ArrayList<>(deletedPendingMembers.size());

    for (DecryptedPendingMemberRemoval member : deletedPendingMembers) {
      ServiceId serviceId = ServiceId.parseOrNull(member.getServiceIdBytes());

      if(serviceId != null) {
        serviceIdList.add(serviceId);
      }
    }

    return serviceIdList;
  }

  /**
   * Will not return any non-decryptable member ACIs.
   */
  public static ArrayList<ServiceId> removedRequestingMembersServiceIdList(DecryptedGroupChange groupChange) {
    List<ByteString>     deleteRequestingMembers = groupChange.getDeleteRequestingMembersList();
    ArrayList<ServiceId> serviceIdList           = new ArrayList<>(deleteRequestingMembers.size());

    for (ByteString member : deleteRequestingMembers) {
      ServiceId serviceId = ServiceId.parseOrNull(member);

      if(serviceId != null) {
        serviceIdList.add(serviceId);
      }
    }

    return serviceIdList;
  }

  public static Set<ServiceId> bannedMembersToServiceIdSet(Collection<DecryptedBannedMember> membersList) {
    Set<ServiceId> serviceIdSet = new HashSet<>(membersList.size());

    for (DecryptedBannedMember member : membersList) {
      ServiceId serviceId = ServiceId.parseOrNull(member.getServiceIdBytes());
      if (serviceId != null) {
        serviceIdSet.add(serviceId);
      }
    }

    return serviceIdSet;
  }

  /**
   * The ACI of the member that made the change.
   */
  public static Optional<ServiceId> editorServiceId(DecryptedGroupChange change) {
    return Optional.ofNullable(change != null ? ServiceId.parseOrNull(change.getEditorServiceIdBytes()) : null);
  }

  public static Optional<DecryptedMember> findMemberByAci(Collection<DecryptedMember> members, ACI aci) {
    ByteString aciBytes = aci.toByteString();

    for (DecryptedMember member : members) {
      if (aciBytes.equals(member.getAciBytes())) {
        return Optional.of(member);
      }
    }

    return Optional.empty();
  }

  public static Optional<DecryptedPendingMember> findPendingByServiceId(Collection<DecryptedPendingMember> members, ServiceId serviceId) {
    ByteString serviceIdBinary = serviceId.toByteString();

    for (DecryptedPendingMember member : members) {
      if (serviceIdBinary.equals(member.getServiceIdBytes())) {
        return Optional.of(member);
      }
    }

    return Optional.empty();
  }

  public static Optional<DecryptedPendingMember> findPendingByServiceIds(Collection<DecryptedPendingMember> members, ServiceIds serviceIds) {
    for (DecryptedPendingMember member : members) {
      if (serviceIds.matches(member.getServiceIdBytes())) {
        return Optional.of(member);
      }
    }

    return Optional.empty();
  }

  private static int findPendingIndexByServiceIdCipherText(List<DecryptedPendingMember> members, ByteString cipherText) {
    for (int i = 0; i < members.size(); i++) {
      DecryptedPendingMember member = members.get(i);
      if (cipherText.equals(member.getServiceIdCipherText())) {
        return i;
      }
    }

    return -1;
  }

  private static int findPendingIndexByServiceId(List<DecryptedPendingMember> members, ByteString serviceIdBinary) {
    for (int i = 0; i < members.size(); i++) {
      DecryptedPendingMember member = members.get(i);
      if (serviceIdBinary.equals(member.getServiceIdBytes())) {
        return i;
      }
    }

    return -1;
  }

  public static Optional<DecryptedRequestingMember> findRequestingByAci(Collection<DecryptedRequestingMember> members, ACI aci) {
    ByteString aciBytes = aci.toByteString();

    for (DecryptedRequestingMember member : members) {
      if (aciBytes.equals(member.getAciBytes())) {
        return Optional.of(member);
      }
    }

    return Optional.empty();
  }

  public static Optional<DecryptedRequestingMember> findRequestingByServiceIds(Collection<DecryptedRequestingMember> members, ServiceIds serviceIds) {
    for (DecryptedRequestingMember member : members) {
      if (serviceIds.matches(member.getAciBytes())) {
        return Optional.of(member);
      }
    }

    return Optional.empty();
  }

  public static boolean isPendingOrRequesting(DecryptedGroup group, ServiceIds serviceIds) {
    return findPendingByServiceIds(group.getPendingMembersList(), serviceIds).isPresent() ||
           findRequestingByServiceIds(group.getRequestingMembersList(), serviceIds).isPresent();
  }

  public static boolean isRequesting(DecryptedGroup group, ACI aci) {
    return findRequestingByAci(group.getRequestingMembersList(), aci).isPresent();
  }

  /**
   * Removes the aci from the full members of a group.
   * <p>
   * Generally not expected to have to do this, just in the case of leaving a group where you cannot
   * get the new group state as you are not in the group any longer.
   */
  public static DecryptedGroup removeMember(DecryptedGroup group, ACI aci, int revision) {
    DecryptedGroup.Builder     builder          = DecryptedGroup.newBuilder(group);
    ByteString                 aciByteString    = aci.toByteString();
    boolean                    removed          = false;
    ArrayList<DecryptedMember> decryptedMembers = new ArrayList<>(builder.getMembersList());
    Iterator<DecryptedMember>  membersList      = decryptedMembers.iterator();

    while (membersList.hasNext()) {
      if (aciByteString.equals(membersList.next().getAciBytes())) {
        membersList.remove();
        removed = true;
      }
    }

    if (removed) {
      return builder.clearMembers()
                    .addAllMembers(decryptedMembers)
                    .setRevision(revision)
                    .build();
    } else {
      return group;
    }
  }

  public static DecryptedGroup apply(DecryptedGroup group, DecryptedGroupChange change)
      throws NotAbleToApplyGroupV2ChangeException
  {
    if (change.getRevision() != group.getRevision() + 1) {
      throw new NotAbleToApplyGroupV2ChangeException();
    }

    return applyWithoutRevisionCheck(group, change);
  }

  public static DecryptedGroup applyWithoutRevisionCheck(DecryptedGroup group, DecryptedGroupChange change)
      throws NotAbleToApplyGroupV2ChangeException
  {
    DecryptedGroup.Builder builder = DecryptedGroup.newBuilder(group)
                                                   .setRevision(change.getRevision());

    applyAddMemberAction(builder, change.getNewMembersList());

    applyDeleteMemberActions(builder, change.getDeleteMembersList());

    applyModifyMemberRoleActions(builder, change.getModifyMemberRolesList());

    applyModifyMemberProfileKeyActions(builder, change.getModifiedProfileKeysList());

    applyAddPendingMemberActions(builder, change.getNewPendingMembersList());

    applyDeletePendingMemberActions(builder, change.getDeletePendingMembersList());

    applyPromotePendingMemberActions(builder, change.getPromotePendingMembersList());

    applyModifyTitleAction(builder, change);

    applyModifyDescriptionAction(builder, change);

    applyModifyIsAnnouncementGroupAction(builder, change);

    applyModifyAvatarAction(builder, change);

    applyModifyDisappearingMessagesTimerAction(builder, change);

    applyModifyAttributesAccessControlAction(builder, change);

    applyModifyMembersAccessControlAction(builder, change);

    applyModifyAddFromInviteLinkAccessControlAction(builder, change);

    applyAddRequestingMembers(builder, change.getNewRequestingMembersList());

    applyDeleteRequestingMembers(builder, change.getDeleteRequestingMembersList());

    applyPromoteRequestingMemberActions(builder, change.getPromoteRequestingMembersList());

    applyInviteLinkPassword(builder, change);

    applyAddBannedMembersActions(builder, change.getNewBannedMembersList());

    applyDeleteBannedMembersActions(builder, change.getDeleteBannedMembersList());

    applyPromotePendingPniAciMemberActions(builder, change.getPromotePendingPniAciMembersList());

    return builder.build();
  }

  private static void applyAddMemberAction(DecryptedGroup.Builder builder, List<DecryptedMember> newMembersList) {
    if (newMembersList.isEmpty()) return;

    LinkedHashMap<ByteString, DecryptedMember> members = new LinkedHashMap<>();

    for (DecryptedMember member : builder.getMembersList()) {
      members.put(member.getAciBytes(), member);
    }

    for (DecryptedMember member : newMembersList) {
      members.put(member.getAciBytes(), member);
    }

    builder.clearMembers();
    builder.addAllMembers(members.values());

    removePendingAndRequestingMembersNowInGroup(builder);
  }

  protected static void applyDeleteMemberActions(DecryptedGroup.Builder builder, List<ByteString> deleteMembersList) {
    for (ByteString removedMember : deleteMembersList) {
      int index = indexOfAci(builder.getMembersList(), removedMember);

      if (index == -1) {
        Log.w(TAG, "Deleted member on change not found in group");
        continue;
      }

      builder.removeMembers(index);
    }
  }

  private static void applyModifyMemberRoleActions(DecryptedGroup.Builder builder, List<DecryptedModifyMemberRole> modifyMemberRolesList) throws NotAbleToApplyGroupV2ChangeException {
    for (DecryptedModifyMemberRole modifyMemberRole : modifyMemberRolesList) {
      int index = indexOfAci(builder.getMembersList(), modifyMemberRole.getAciBytes());

      if (index == -1) {
        throw new NotAbleToApplyGroupV2ChangeException();
      }

      Member.Role role = modifyMemberRole.getRole();

      ensureKnownRole(role);

      builder.setMembers(index, DecryptedMember.newBuilder(builder.getMembers(index))
                                               .setRole(role));
    }
  }

  private static void applyModifyMemberProfileKeyActions(DecryptedGroup.Builder builder, List<DecryptedMember> modifiedProfileKeysList) throws NotAbleToApplyGroupV2ChangeException {
    for (DecryptedMember modifyProfileKey : modifiedProfileKeysList) {
      int index = indexOfAci(builder.getMembersList(), modifyProfileKey.getAciBytes());

      if (index == -1) {
        throw new NotAbleToApplyGroupV2ChangeException();
      }

      builder.setMembers(index, withNewProfileKey(builder.getMembers(index), modifyProfileKey.getProfileKey()));
    }
  }

  private static void applyAddPendingMemberActions(DecryptedGroup.Builder builder, List<DecryptedPendingMember> newPendingMembersList) throws NotAbleToApplyGroupV2ChangeException {
    Set<ByteString> fullMemberSet            = getMemberAciSet(builder.getMembersList());
    Set<ByteString> pendingMemberCipherTexts = getPendingMemberCipherTextSet(builder.getPendingMembersList());

    for (DecryptedPendingMember pendingMember : newPendingMembersList) {
      if (fullMemberSet.contains(pendingMember.getServiceIdBytes())) {
        throw new NotAbleToApplyGroupV2ChangeException();
      }

      if (!pendingMemberCipherTexts.contains(pendingMember.getServiceIdCipherText())) {
        builder.addPendingMembers(pendingMember);
      }
    }
  }

  protected static void applyDeletePendingMemberActions(DecryptedGroup.Builder builder, List<DecryptedPendingMemberRemoval> deletePendingMembersList) {
    for (DecryptedPendingMemberRemoval removedMember : deletePendingMembersList) {
      int index = findPendingIndexByServiceIdCipherText(builder.getPendingMembersList(), removedMember.getServiceIdCipherText());

      if (index == -1) {
        Log.w(TAG, "Deleted pending member on change not found in group");
        continue;
      }

      builder.removePendingMembers(index);
    }
  }

  protected static void applyPromotePendingMemberActions(DecryptedGroup.Builder builder, List<DecryptedMember> promotePendingMembersList) throws NotAbleToApplyGroupV2ChangeException {
    for (DecryptedMember newMember : promotePendingMembersList) {
      int index = findPendingIndexByServiceId(builder.getPendingMembersList(), newMember.getAciBytes());

      if (index == -1) {
        throw new NotAbleToApplyGroupV2ChangeException();
      }

      builder.removePendingMembers(index);
      builder.addMembers(newMember);
    }
  }

  protected static void applyModifyTitleAction(DecryptedGroup.Builder builder, DecryptedGroupChange change) {
    if (change.hasNewTitle()) {
      builder.setTitle(change.getNewTitle().getValue());
    }
  }

  protected static void applyModifyDescriptionAction(DecryptedGroup.Builder builder, DecryptedGroupChange change) {
    if (change.hasNewDescription()) {
      builder.setDescription(change.getNewDescription().getValue());
    }
  }

  protected static void applyModifyIsAnnouncementGroupAction(DecryptedGroup.Builder builder, DecryptedGroupChange change) {
    if (change.getNewIsAnnouncementGroup() != EnabledState.UNKNOWN) {
      builder.setIsAnnouncementGroup(change.getNewIsAnnouncementGroup());
    }
  }


  protected static void applyModifyAvatarAction(DecryptedGroup.Builder builder, DecryptedGroupChange change) {
    if (change.hasNewAvatar()) {
      builder.setAvatar(change.getNewAvatar().getValue());
    }
  }

  protected static void applyModifyDisappearingMessagesTimerAction(DecryptedGroup.Builder builder, DecryptedGroupChange change) {
    if (change.hasNewTimer()) {
      builder.setDisappearingMessagesTimer(change.getNewTimer());
    }
  }

  protected static void applyModifyAttributesAccessControlAction(DecryptedGroup.Builder builder, DecryptedGroupChange change) {
    AccessControl.AccessRequired newAccessLevel = change.getNewAttributeAccess();

    if (newAccessLevel != AccessControl.AccessRequired.UNKNOWN) {
      builder.setAccessControl(AccessControl.newBuilder(builder.getAccessControl())
                                            .setAttributesValue(change.getNewAttributeAccessValue()));
    }
  }

  protected static void applyModifyMembersAccessControlAction(DecryptedGroup.Builder builder, DecryptedGroupChange change) {
    AccessControl.AccessRequired newAccessLevel = change.getNewMemberAccess();

    if (newAccessLevel != AccessControl.AccessRequired.UNKNOWN) {
      builder.setAccessControl(AccessControl.newBuilder(builder.getAccessControl())
                                            .setMembersValue(change.getNewMemberAccessValue()));
    }
  }

  protected static void applyModifyAddFromInviteLinkAccessControlAction(DecryptedGroup.Builder builder, DecryptedGroupChange change) {
    AccessControl.AccessRequired newAccessLevel = change.getNewInviteLinkAccess();

    if (newAccessLevel != AccessControl.AccessRequired.UNKNOWN) {
      builder.setAccessControl(AccessControl.newBuilder(builder.getAccessControl())
                                            .setAddFromInviteLink(newAccessLevel));
    }
  }

  private static void applyAddRequestingMembers(DecryptedGroup.Builder builder, List<DecryptedRequestingMember> newRequestingMembers) {
    builder.addAllRequestingMembers(newRequestingMembers);
  }

  private static void applyDeleteRequestingMembers(DecryptedGroup.Builder builder, List<ByteString> deleteRequestingMembersList) {
    for (ByteString removedMember : deleteRequestingMembersList) {
      int index = indexOfAciInRequestingList(builder.getRequestingMembersList(), removedMember);

      if (index == -1) {
        Log.w(TAG, "Deleted member on change not found in group");
        continue;
      }

      builder.removeRequestingMembers(index);
    }
  }

  private static void applyPromoteRequestingMemberActions(DecryptedGroup.Builder builder, List<DecryptedApproveMember> promoteRequestingMembers) throws NotAbleToApplyGroupV2ChangeException {
    for (DecryptedApproveMember approvedMember : promoteRequestingMembers) {
      int index = indexOfAciInRequestingList(builder.getRequestingMembersList(), approvedMember.getAciBytes());

      if (index == -1) {
        Log.w(TAG, "Deleted member on change not found in group");
        continue;
      }

      DecryptedRequestingMember requestingMember = builder.getRequestingMembers(index);
      Member.Role               role             = approvedMember.getRole();

      ensureKnownRole(role);

      builder.removeRequestingMembers(index)
             .addMembers(DecryptedMember.newBuilder()
                                        .setAciBytes(approvedMember.getAciBytes())
                                        .setProfileKey(requestingMember.getProfileKey())
                                        .setRole(role));
    }
  }

  private static void applyInviteLinkPassword(DecryptedGroup.Builder builder, DecryptedGroupChange change) {
    if (!change.getNewInviteLinkPassword().isEmpty()) {
      builder.setInviteLinkPassword(change.getNewInviteLinkPassword());
    }
  }

  private static void applyAddBannedMembersActions(DecryptedGroup.Builder builder, List<DecryptedBannedMember> newBannedMembersList) {
    Set<ByteString> bannedMemberServiceIdSet = getBannedMemberServiceIdSet(builder.getBannedMembersList());

    for (DecryptedBannedMember member : newBannedMembersList) {
      if (bannedMemberServiceIdSet.contains(member.getServiceIdBytes())) {
        Log.w(TAG, "Banned member already in banned list");
      } else {
        builder.addBannedMembers(member);
      }
    }
  }

  private static void applyDeleteBannedMembersActions(DecryptedGroup.Builder builder, List<DecryptedBannedMember> deleteMembersList) {
    for (DecryptedBannedMember removedMember : deleteMembersList) {
      int index = indexOfServiceIdInBannedMemberList(builder.getBannedMembersList(), removedMember.getServiceIdBytes());

      if (index == -1) {
        Log.w(TAG, "Deleted banned member on change not found in banned list");
        continue;
      }

      builder.removeBannedMembers(index);
    }
  }

  protected static void applyPromotePendingPniAciMemberActions(DecryptedGroup.Builder builder, List<DecryptedMember> promotePendingPniAciMembersList) throws NotAbleToApplyGroupV2ChangeException {
    for (DecryptedMember newMember : promotePendingPniAciMembersList) {
      int index = findPendingIndexByServiceId(builder.getPendingMembersList(), newMember.getPniBytes());

      if (index == -1) {
        throw new NotAbleToApplyGroupV2ChangeException();
      }

      builder.removePendingMembers(index);
      builder.addMembers(newMember);
    }
  }

  private static DecryptedMember withNewProfileKey(DecryptedMember member, ByteString profileKey) {
    return DecryptedMember.newBuilder(member)
                          .setProfileKey(profileKey)
                          .build();
  }

  private static Set<ByteString> getMemberAciSet(List<DecryptedMember> membersList) {
    Set<ByteString> memberAcis = new HashSet<>(membersList.size());

    for (DecryptedMember members : membersList) {
      memberAcis.add(members.getAciBytes());
    }

    return memberAcis;
  }

    private static Set<ByteString> getPendingMemberCipherTextSet(List<DecryptedPendingMember> pendingMemberList) {
    Set<ByteString> pendingMemberCipherTexts = new HashSet<>(pendingMemberList.size());

    for (DecryptedPendingMember pendingMember : pendingMemberList) {
      pendingMemberCipherTexts.add(pendingMember.getServiceIdCipherText());
    }

    return pendingMemberCipherTexts;
  }

  private static Set<ByteString> getBannedMemberServiceIdSet(List<DecryptedBannedMember> bannedMemberList) {
    Set<ByteString> memberServiceIds = new HashSet<>(bannedMemberList.size());

    for (DecryptedBannedMember member : bannedMemberList) {
      memberServiceIds.add(member.getServiceIdBytes());
    }

    return memberServiceIds;
  }

  private static void removePendingAndRequestingMembersNowInGroup(DecryptedGroup.Builder builder) {
    Set<ByteString> allMembers = membersToAciByteStringSet(builder.getMembersList());

    for (int i = builder.getPendingMembersCount() - 1; i >= 0; i--) {
      DecryptedPendingMember pendingMember = builder.getPendingMembers(i);
      if (allMembers.contains(pendingMember.getServiceIdBytes())) {
        builder.removePendingMembers(i);
      }
    }

    for (int i = builder.getRequestingMembersCount() - 1; i >= 0; i--) {
      DecryptedRequestingMember requestingMember = builder.getRequestingMembers(i);
      if (allMembers.contains(requestingMember.getAciBytes())) {
        builder.removeRequestingMembers(i);
      }
    }
  }

  private static void ensureKnownRole(Member.Role role) throws NotAbleToApplyGroupV2ChangeException {
    if (role != Member.Role.ADMINISTRATOR && role != Member.Role.DEFAULT) {
      throw new NotAbleToApplyGroupV2ChangeException();
    }
  }

  private static int indexOfAci(List<DecryptedMember> memberList, ByteString aci) {
    for (int i = 0; i < memberList.size(); i++) {
      if (aci.equals(memberList.get(i).getAciBytes())) {
        return i;
      }
    }
    return -1;
  }

  private static int indexOfAciInRequestingList(List<DecryptedRequestingMember> memberList, ByteString aci) {
    for (int i = 0; i < memberList.size(); i++) {
      if (aci.equals(memberList.get(i).getAciBytes())) {
        return i;
      }
    }
    return -1;
  }

  private static int indexOfServiceIdInBannedMemberList(List<DecryptedBannedMember> memberList, ByteString serviceIdBinary) {
    for (int i = 0; i < memberList.size(); i++) {
      if (serviceIdBinary.equals(memberList.get(i).getServiceIdBytes())) {
        return i;
      }
    }
    return -1;
  }

  public static boolean changeIsEmpty(DecryptedGroupChange change) {
    return change.getModifiedProfileKeysCount()   == 0 && // field 6
           changeIsEmptyExceptForProfileKeyChanges(change);
  }

  /*
   * When updating this, update {@link #changeIsEmptyExceptForBanChangesAndOptionalProfileKeyChanges(DecryptedGroupChange)}
   */
  public static boolean changeIsEmptyExceptForProfileKeyChanges(DecryptedGroupChange change) {
    return change.getNewMembersCount() == 0 &&                // field 3
           change.getDeleteMembersCount() == 0 &&             // field 4
           change.getModifyMemberRolesCount() == 0 &&         // field 5
           change.getNewPendingMembersCount() == 0 &&         // field 7
           change.getDeletePendingMembersCount() == 0 &&      // field 8
           change.getPromotePendingMembersCount() == 0 &&     // field 9
           !change.hasNewTitle() &&                           // field 10
           !change.hasNewAvatar() &&                          // field 11
           !change.hasNewTimer() &&                           // field 12
           isEmpty(change.getNewAttributeAccess()) &&         // field 13
           isEmpty(change.getNewMemberAccess()) &&            // field 14
           isEmpty(change.getNewInviteLinkAccess()) &&        // field 15
           change.getNewRequestingMembersCount() == 0 &&      // field 16
           change.getDeleteRequestingMembersCount() == 0 &&   // field 17
           change.getPromoteRequestingMembersCount() == 0 &&  // field 18
           change.getNewInviteLinkPassword().size() == 0 &&   // field 19
           !change.hasNewDescription() &&                     // field 20
           isEmpty(change.getNewIsAnnouncementGroup()) &&     // field 21
           change.getNewBannedMembersCount() == 0 &&          // field 22
           change.getDeleteBannedMembersCount() == 0 &&       // field 23
           change.getPromotePendingPniAciMembersCount() == 0; // field 24
  }

  public static boolean changeIsEmptyExceptForBanChangesAndOptionalProfileKeyChanges(DecryptedGroupChange change) {
    return (change.getNewBannedMembersCount() != 0 || change.getDeleteBannedMembersCount() != 0) &&
           change.getNewMembersCount() == 0 &&                // field 3
           change.getDeleteMembersCount() == 0 &&             // field 4
           change.getModifyMemberRolesCount() == 0 &&         // field 5
           change.getNewPendingMembersCount() == 0 &&         // field 7
           change.getDeletePendingMembersCount() == 0 &&      // field 8
           change.getPromotePendingMembersCount() == 0 &&     // field 9
           !change.hasNewTitle() &&                           // field 10
           !change.hasNewAvatar() &&                          // field 11
           !change.hasNewTimer() &&                           // field 12
           isEmpty(change.getNewAttributeAccess()) &&         // field 13
           isEmpty(change.getNewMemberAccess()) &&            // field 14
           isEmpty(change.getNewInviteLinkAccess()) &&        // field 15
           change.getNewRequestingMembersCount() == 0 &&      // field 16
           change.getDeleteRequestingMembersCount() == 0 &&   // field 17
           change.getPromoteRequestingMembersCount() == 0 &&  // field 18
           change.getNewInviteLinkPassword().size() == 0 &&   // field 19
           !change.hasNewDescription() &&                     // field 20
           isEmpty(change.getNewIsAnnouncementGroup()) &&     // field 21
           change.getPromotePendingPniAciMembersCount() == 0; // field 24
  }

  static boolean isEmpty(AccessControl.AccessRequired newAttributeAccess) {
    return newAttributeAccess == AccessControl.AccessRequired.UNKNOWN;
  }

  static boolean isEmpty(EnabledState enabledState) {
    return enabledState == EnabledState.UNKNOWN;
  }

  public static boolean changeIsSilent(DecryptedGroupChange plainGroupChange) {
    return changeIsEmptyExceptForProfileKeyChanges(plainGroupChange) || changeIsEmptyExceptForBanChangesAndOptionalProfileKeyChanges(plainGroupChange);
  }
}
