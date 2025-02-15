/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.query.planner.plannode;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.calcite.rel.RelDistribution;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.pinot.common.utils.DataSchema;
import org.apache.pinot.query.planner.logical.RexExpression;
import org.apache.pinot.query.planner.partitioning.KeySelector;
import org.apache.pinot.query.planner.serde.ProtoProperties;


public class MailboxReceiveNode extends AbstractPlanNode {
  @ProtoProperties
  private int _senderStageId;
  @ProtoProperties
  private RelDistribution.Type _exchangeType;
  @ProtoProperties
  private KeySelector<Object[], Object[]> _partitionKeySelector;
  @ProtoProperties
  private List<RexExpression> _collationKeys;
  @ProtoProperties
  private List<RelFieldCollation.Direction> _collationDirections;
  @ProtoProperties
  private boolean _isSortOnSender;
  @ProtoProperties
  private boolean _isSortOnReceiver;

  // this is only available during planning and should not be relied
  // on in any post-serialization code
  private transient PlanNode _sender;

  public MailboxReceiveNode(int planFragmentId) {
    super(planFragmentId);
  }

  public MailboxReceiveNode(int planFragmentId, DataSchema dataSchema, int senderStageId,
      RelDistribution.Type exchangeType, @Nullable KeySelector<Object[], Object[]> partitionKeySelector,
      @Nullable List<RelFieldCollation> fieldCollations, boolean isSortOnSender, boolean isSortOnReceiver,
      PlanNode sender) {
    super(planFragmentId, dataSchema);
    _senderStageId = senderStageId;
    _exchangeType = exchangeType;
    _partitionKeySelector = partitionKeySelector;
    if (!CollectionUtils.isEmpty(fieldCollations)) {
      _collationKeys = new ArrayList<>(fieldCollations.size());
      _collationDirections = new ArrayList<>(fieldCollations.size());
      for (RelFieldCollation fieldCollation : fieldCollations) {
        _collationDirections.add(fieldCollation.getDirection());
        _collationKeys.add(new RexExpression.InputRef(fieldCollation.getFieldIndex()));
      }
    } else {
      _collationKeys = Collections.emptyList();
      _collationDirections = Collections.emptyList();
    }
    _isSortOnSender = isSortOnSender;
    Preconditions.checkState(!isSortOnSender, "Input shouldn't be sorted as ordering on send is not yet implemented!");
    _isSortOnReceiver = isSortOnReceiver;
    _sender = sender;
  }

  public void setSenderStageId(Integer senderStageId) {
    _senderStageId = senderStageId;
  }

  public int getSenderStageId() {
    return _senderStageId;
  }

  public void setExchangeType(RelDistribution.Type exchangeType) {
    _exchangeType = exchangeType;
  }

  public RelDistribution.Type getExchangeType() {
    return _exchangeType;
  }

  public KeySelector<Object[], Object[]> getPartitionKeySelector() {
    return _partitionKeySelector;
  }

  public List<RexExpression> getCollationKeys() {
    return _collationKeys;
  }

  public List<RelFieldCollation.Direction> getCollationDirections() {
    return _collationDirections;
  }

  public boolean isSortOnSender() {
    return _isSortOnSender;
  }

  public boolean isSortOnReceiver() {
    return _isSortOnReceiver;
  }

  public PlanNode getSender() {
    return _sender;
  }

  @Override
  public String explain() {
    return "MAIL_RECEIVE(" + _exchangeType + ")";
  }

  @Override
  public <T, C> T visit(PlanNodeVisitor<T, C> visitor, C context) {
    return visitor.visitMailboxReceive(this, context);
  }
}
