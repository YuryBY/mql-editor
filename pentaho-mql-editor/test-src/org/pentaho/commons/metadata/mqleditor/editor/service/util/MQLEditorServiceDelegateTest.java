/*
 * This program is free software; you can redistribute it and/or modify it under the
 *  terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 *  Foundation.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this
 *  program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  or from the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  Copyright (c) 2006 - 2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.commons.metadata.mqleditor.editor.service.util;

import org.junit.Assert;
import org.junit.Test;
import org.pentaho.commons.metadata.mqleditor.AggType;
import org.pentaho.metadata.model.Domain;
import org.pentaho.metadata.model.concept.types.AggregationType;
import org.pentaho.metadata.model.concept.types.LocalizedString;
import org.pentaho.metadata.repository.IMetadataDomainRepository;
import org.pentaho.metadata.repository.InMemoryMetadataDomainRepository;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MQLEditorServiceDelegateTest {
  @Test
  public void convertNewThinAggregationTypeTest() {
    MQLEditorServiceDelegate mqlESD = new MQLEditorServiceDelegate();

    Assert.assertEquals( mqlESD.convertNewThinAggregationType( null ), AggType.NONE );

    Assert.assertEquals( mqlESD.convertNewThinAggregationType( AggregationType.COUNT ), AggType.COUNT );

    Assert.assertEquals( mqlESD.convertNewThinAggregationType( AggregationType.COUNT_DISTINCT ), AggType.COUNT_DISTINCT );

    Assert.assertEquals( mqlESD.convertNewThinAggregationType( AggregationType.AVERAGE ), AggType.AVERAGE );

    Assert.assertEquals( mqlESD.convertNewThinAggregationType( AggregationType.MINIMUM ), AggType.MIN );

    Assert.assertEquals( mqlESD.convertNewThinAggregationType( AggregationType.MAXIMUM ), AggType.MAX );

    Assert.assertEquals( mqlESD.convertNewThinAggregationType( AggregationType.SUM ), AggType.SUM );

    Assert.assertEquals( mqlESD.convertNewThinAggregationType( AggregationType.NONE ), AggType.NONE );
  }

  @Test
  public void testConcurrency() throws Exception {
    String domainIdPrefix = "id";
    //init repo
    IMetadataDomainRepository repo = new InMemoryMetadataDomainRepository();
    for ( int i = 0; i < 50; i++ ) {
      Domain domain = new Domain();
      LocalizedString name = new LocalizedString();
      name.setString( "US", String.valueOf( i + 1 ) );
      domain.setId( domainIdPrefix + String.valueOf( i + 1 ) );
      domain.setName( name );
      repo.storeDomain( domain, false );
    }

    MQLEditorServiceDelegate service = new MQLEditorServiceDelegate( repo );

    int poolSize = 10;
    ExecutorService executorService = Executors.newFixedThreadPool( poolSize );

    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    //redirect the System-output to a variable to use it for test purposes
    //System.setErr( new PrintStream( outContent ) );
    List<Future<Boolean>> results = new ArrayList<>();
    for ( int i = 0; i < poolSize; i++ ) {
      results.add( executorService.submit( new Callable<Boolean>() {
        public Boolean call() throws Exception {
          int size = service.getMetadataDomains().size();
          for ( int i = 0; i < size; i++ ) {
            try {
              String id = domainIdPrefix + String.valueOf( i + 1 );
              service.getDomainByName( id );
              service.addThinDomain( id );
              //System.out.println(String.valueOf( i + 1 ));
            } catch ( Exception e ) {
              return false;
            }
          }
          return true;
        }
      } ) );
    }
    for ( Future<Boolean> result : results ) {
      Assert.assertTrue( result.get() );
    }
    executorService.shutdown();

    //Assert.assertTrue( outContent.toString(), outContent.size() == 0 );
  }
}
