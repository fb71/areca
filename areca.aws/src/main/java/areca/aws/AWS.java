/*
 * Copyright (C) 2023, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package areca.aws;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;

/**
 *
 * @author Falko Bräutigam
 */
public class AWS {

    private static final XLogger LOG = XLogger.get( AWS.class );

    protected static final Region REGION = Region.EU_CENTRAL_1;

    protected Ec2Client ec2;


    public AWS() {
        this.ec2 = Ec2Client.builder()
                .region( REGION )
                .credentialsProvider( ProfileCredentialsProvider.create( "default" ) )
                .build();
    }


    public void dispose() {
        ec2.close();
        ec2 = null;
    }


    public boolean isInstanceRunning( String ec2InstanceId ) {
        var instance = describeInstances( ec2InstanceId );
        String state = instance.state().name().name();
        return state.compareTo( "RUNNING" ) == 0;
    }


    public Instance describeInstances( String ec2InstanceId ) {
        var request = DescribeInstancesRequest.builder()
                .instanceIds( ec2InstanceId )
                .build();

        var response = ec2.describeInstances( request );
        Instance instance = response.reservations().get( 0 ).instances().get( 0 );
        //LOG.info( "Image id: %s", instance.imageId() );
        LOG.info( "Instance type: %s =========", instance.instanceType() );
        LOG.info( "Instance state: %s", instance.state().name() );
        LOG.info( "Instance address: %s", instance.publicIpAddress() );
        return instance;
    }


    /**
     * Start the given instance and wait until the state changed to RUNNING.
     */
    public void startInstance( String ec2InstanceId ) {
        var ec2Waiter = Ec2Waiter.builder()
                .overrideConfiguration( b -> b.maxAttempts( 100 ) )
                .client( ec2 )
                .build();

        var request = StartInstancesRequest.builder()
                .instanceIds( ec2InstanceId )
                .build();

        LOG.info( "Use an Ec2Waiter to wait for the instance to run..." );
        ec2.startInstances( request );
        var instanceRequest = DescribeInstancesRequest.builder()
                .instanceIds( ec2InstanceId )
                .build();

        ec2Waiter.waitUntilInstanceRunning( instanceRequest );
        //waiterResponse.matched().response().ifPresent( System.out::println );
        LOG.info( "Successfully started instance: %s", ec2InstanceId );
    }


    /**
     * Stop the given instance and wait until the state changed to RUNNING.
     */
    public void stopInstance( String ec2InstanceId ) {
        var ec2Waiter = Ec2Waiter.builder()
                .overrideConfiguration( b -> b.maxAttempts( 100 ) )
                .client( ec2 )
                .build();

        LOG.info( "Use an Ec2Waiter to wait for the instance to stop..." );
        ec2.stopInstances( StopInstancesRequest.builder()
                .instanceIds( ec2InstanceId )
                .build() );

        ec2Waiter.waitUntilInstanceStopped( DescribeInstancesRequest.builder()
                .instanceIds( ec2InstanceId )
                .build() );
        //waiterResponse.matched().response().ifPresent( System.out::println );
        LOG.info( "Successfully stopped instance: %s", ec2InstanceId );
    }


    public String runInstance( String instanceType, String keyName, String groupName, String amiId ) {
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .instanceType( instanceType )
                .keyName( keyName )
                .securityGroups( groupName )
                .maxCount( 1 )
                .minCount( 1 )
                .imageId( amiId )
                .build();

        RunInstancesResponse response = ec2.runInstances( runRequest );
        String instanceId = response.instances().get( 0 ).instanceId();
        System.out.println( "Successfully started EC2 instance " + instanceId + " based on AMI " + amiId );
        return instanceId;
    }


    /**
     * Test
     */
    public static void main( String[] args ) {
        LOG.info( "Init ..." );
        new AWS().describeInstances( "i-0ef343b852bd58970" );
    }

}
