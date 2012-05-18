/*
 * Copyright 2011 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.event.monitor.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.InputSupplier;
import com.proofpoint.log.Logger;

import javax.inject.Inject;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import static com.proofpoint.event.monitor.s3.S3StorageHelper.buildS3Location;
import static com.proofpoint.event.monitor.s3.S3StorageHelper.getS3Bucket;
import static com.proofpoint.event.monitor.s3.S3StorageHelper.getS3ObjectKey;

public class S3StorageSystem
{
    private static final Logger log = Logger.get(S3StorageSystem.class);
    private final AmazonS3 s3Service;

    @Inject
    public S3StorageSystem(AmazonS3 s3Service)
    {
        Preconditions.checkNotNull(s3Service, "s3Service is null");
        this.s3Service = s3Service;
    }

    public List<URI> listDirectoriesNewestFirst(URI storageArea)
    {
        S3StorageHelper.checkValidS3Uri(storageArea);

        String bucket = getS3Bucket(storageArea);
        String key = getS3ObjectKey(storageArea);
        Iterator<String> iter = new S3PrefixListing(s3Service,
                new ListObjectsRequest(bucket, key, null, "/", null)).iterator();

        ImmutableList.Builder<URI> builder = ImmutableList.builder();
        while (iter.hasNext()) {
            builder.add(buildS3Location("s3://", bucket, iter.next()));
        }
        return builder.build().reverse();
    }

    public List<URI> listObjects(URI storageArea)
    {
        S3StorageHelper.checkValidS3Uri(storageArea);

        String s3Path = getS3ObjectKey(storageArea);
        Iterable<S3ObjectSummary> objectListing = new S3ObjectListing(s3Service,
                new ListObjectsRequest(getS3Bucket(storageArea), s3Path, null, "/", null));

        ImmutableList.Builder<URI> builder = ImmutableList.builder();
        for (S3ObjectSummary summary : objectListing) {
            builder.add(buildS3Location(storageArea, summary.getKey().substring(s3Path.length())));
        }
        return builder.build();
    }

    public InputSupplier<InputStream> getInputSupplier(URI target)
    {
        return new S3InputSupplier(s3Service, target);
    }
}
