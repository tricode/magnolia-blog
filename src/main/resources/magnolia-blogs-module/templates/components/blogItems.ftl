[#if (!ctx.parameters.name?has_content)]
    [#if (content.blogGroup?has_content) ]
        [#assign blogGroupPath = cmsfn.contentById(content.blogGroup, "collaboration").@path /]
    [/#if]

    [#assign blogs = model.getBlogs(blogGroupPath!"/", content.maxResultsPerPage!"5") /]

    [#if (blogs)?size > 0 ]
        [#assign pageLink = cmsfn.link(cmsfn.page(content)) /]

        [#if (content.detailsPage?has_content) ]
            [#assign blogDetailPageLink = cmsfn.link(cmsfn.contentByPath(content.detailsPage)) /]
        [/#if]

        <div class="blog-summaries">
            [#list blogs as blog]
            <article class="blog-item">
                [#if (blogDetailPageLink?has_content)]
                    <h2><a href="${blogDetailPageLink}?name=${blog.@name}">${blog.title!"No title found"}</a></h2>
                [#else]
                    <h2>${blog.title!"No title found"}</h2>
                [/#if]

                [#assign categories = model.getBlogCategories(blog) /]
                [#if (categories)?size > 0 ]
                    <div class="postdetails">
                        <ul class="list-unstyled inline-list blog-categories">
                            [#list categories as category]
                            <li>
                                <a href="${pageLink}?category=${category.@path}">
                                    <i class="fa fa-star">${category.@name}</i>
                                </a>
                            </li>
                            [/#list]
                        </ul>
                    </div>
                [/#if]

                <section>
                    ${blog.summary!}
                    [#if (blogDetailPageLink?has_content)]
                        <a class="read-more button tiny" href="${blogDetailPageLink}?name=${blog.@name}">
                            Read more...
                        </a>
                    [/#if]
                </section>

                [#assign author = cmsfn.contentById(blog.author, "contacts" ) /]
                <div class="postdetails">
                    <ul class="list-unstyled inline-list blog-info">
                        <li><i class="fa fa-calendar">${cmsfn.metaData(blog,"mgnl:created")?date("yyyy-MM-dd")}</i></li>
                        <li><i class="fa fa-pencil">${author.firstName} ${author.lastName}</i></li>
                    </ul>
                </div>
            </article>
            [/#list]

            [@renderPagination /]
        </div>
    [#else]
        <div class="blog-summaries">
            <p>No blog entries available</p>
        </div>
    [/#if]
[/#if]

[#macro renderPagination]
    [#assign olderPages = model.pageOlderPosts(blogGroupPath!"/",(content.maxResultsPerPage!"5")?number) /]

    [#if (olderPages > 1)]
        <ul class="pager">

        [#assign hasOlderPages = model.hasOlderPosts(blogGroupPath!"/",(content.maxResultsPerPage!"5")?number) /]
        [#if (hasOlderPages)]
            <a class="button left small" href="${pageLink}?page=${olderPages}">
                <i class="fa fa-chevron-left">Older</i>
            </a>
        [/#if]

        [#assign hasNewerPages =  model.hasNewerPosts() /]
        [#if (hasNewerPages)]
            [#assign newerPages =  model.pageNewerPosts() /]

            <a class="button right small" href="${pageLink}?page=${newerPages}">
                <i class="fa fa-chevron-right">Newer</i>
            </a>
        [/#if]

        </ul>
    [/#if]
[/#macro]